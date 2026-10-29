#!/usr/bin/env python3
"""Shared filesystem/JAR helpers for reproducible sandified ore generation."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import zipfile
from typing import Any


def repo_root() -> Path:
    return Path(__file__).resolve().parents[1]


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def locate_minecraft_jar(root: Path, explicit: str | None = None) -> Path:
    if explicit:
        path = Path(explicit).resolve()
        if not path.is_file():
            raise FileNotFoundError(path)
        return path
    candidates = [
        root / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar",
        root / "build/moddev/artifacts/neoforge-21.1.251-client-extra-aka-minecraft-resources.jar",
    ]
    for path in candidates:
        if path.is_file():
            return path
    raise FileNotFoundError(
        "Minecraft 1.21.1 client resources JAR not found. Pass --minecraft-jar."
    )


def locate_create_jar(root: Path, explicit: str | None = None) -> Path:
    if explicit:
        path = Path(explicit).resolve()
        if not path.is_file():
            raise FileNotFoundError(path)
        return path
    cache = root / ".gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1"
    candidates = sorted(cache.glob("**/*-slim.jar"))
    if not candidates:
        candidates = sorted(cache.glob("**/*.jar"))
    if not candidates:
        raise FileNotFoundError(
            "Create 1.21.1 dependency JAR not found. Pass --create-jar."
        )
    return candidates[-1]


class ResourceArchives:
    def __init__(self, minecraft_jar: Path, create_jar: Path):
        self.archives = {
            "minecraft": zipfile.ZipFile(minecraft_jar),
            "create": zipfile.ZipFile(create_jar),
        }

    def close(self) -> None:
        for archive in self.archives.values():
            archive.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        self.close()

    def read(self, namespace: str, relative_path: str) -> bytes:
        archive = self.archives.get(namespace)
        if archive is None:
            raise KeyError(f"no JAR registered for namespace {namespace!r}")
        return archive.read(f"assets/{namespace}/{relative_path}")

    def json(self, namespace: str, relative_path: str) -> Any:
        return json.loads(self.read(namespace, relative_path))

    def read_data(self, namespace: str, relative_path: str) -> bytes:
        archive = self.archives.get(namespace)
        if archive is None:
            raise KeyError(f"no JAR registered for namespace {namespace!r}")
        return archive.read(f"data/{namespace}/{relative_path}")

    def data_json(self, namespace: str, relative_path: str) -> Any:
        return json.loads(self.read_data(namespace, relative_path))

    def exists(self, namespace: str, relative_path: str) -> bool:
        archive = self.archives.get(namespace)
        if archive is None:
            return False
        try:
            archive.getinfo(f"assets/{namespace}/{relative_path}")
            return True
        except KeyError:
            return False


def split_resource_id(resource_id: str) -> tuple[str, str]:
    if ":" in resource_id:
        namespace, path = resource_id.split(":", 1)
    else:
        namespace, path = "minecraft", resource_id
    return namespace, path


def model_texture_all(archives: ResourceArchives, block_id: str) -> str:
    namespace, path = split_resource_id(block_id)
    blockstate = archives.json(namespace, f"blockstates/{path}.json")
    variants = blockstate.get("variants")
    if variants:
        first = next(iter(variants.values()))
        model_ref = first["model"] if isinstance(first, dict) else first[0]["model"]
    else:
        multipart = blockstate.get("multipart")
        if not multipart:
            raise ValueError(f"{block_id} has neither variants nor multipart")
        apply = multipart[0]["apply"]
        model_ref = apply["model"] if isinstance(apply, dict) else apply[0]["model"]
    model_namespace, model_path = split_resource_id(model_ref)
    model = archives.json(model_namespace, f"models/{model_path}.json")
    textures = model.get("textures", {})
    texture_ref = textures.get("all")
    if texture_ref is None:
        # cube_column blocks such as deepslate expose their representative
        # texture through "side"; their top/bottom palette differs by design.
        texture_ref = textures.get("side")
    if texture_ref is None and textures:
        texture_ref = next(iter(textures.values()))
    if texture_ref is None:
        raise ValueError(f"model {model_ref} for {block_id} has no block texture")
    return texture_ref


def project_model_texture_all(root: Path, block_id: str) -> str:
    namespace, path = split_resource_id(block_id)
    if namespace != "createsandustry":
        raise ValueError(f"expected project namespace, got {block_id}")
    model = read_json(root / f"src/main/resources/assets/{namespace}/models/block/{path}.json")
    textures = model.get("textures", {})
    texture_ref = textures.get("all") or textures.get("side")
    if not texture_ref and textures:
        texture_ref = next(iter(textures.values()))
    if not texture_ref:
        raise ValueError(f"project model {block_id} has no block texture")
    return texture_ref


def texture_bytes(archives: ResourceArchives, texture_id: str) -> bytes:
    namespace, path = split_resource_id(texture_id)
    if namespace == "createsandustry":
        raise ValueError("project texture must be read through project_texture_bytes")
    if not path.startswith("block/"):
        raise ValueError(f"expected block texture, got {texture_id}")
    return archives.read(namespace, f"textures/{path}.png")


def project_texture_bytes(root: Path, texture_id: str) -> bytes:
    namespace, path = split_resource_id(texture_id)
    if namespace != "createsandustry":
        raise ValueError(f"expected project texture, got {texture_id}")
    return (root / f"src/main/resources/assets/{namespace}/textures/{path}.png").read_bytes()


def resolve_texture_bytes(
    root: Path, archives: ResourceArchives, texture_id: str
) -> bytes:
    namespace, _ = split_resource_id(texture_id)
    if namespace == "createsandustry":
        return project_texture_bytes(root, texture_id)
    return texture_bytes(archives, texture_id)
