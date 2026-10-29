#!/usr/bin/env python3
"""Dependency-free PNG reader/writer used by sandified ore generation.

Only the lossless operations needed by Minecraft block textures are supported:
8-bit, non-interlaced PNG color types 0, 2, 3, 4 and 6. Pixels are always
represented as exact (R, G, B, A) integer tuples, so generation never applies
color interpolation or resampling.
"""
from __future__ import annotations

from dataclasses import dataclass
import struct
import zlib


PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


@dataclass
class PngImage:
    width: int
    height: int
    pixels: list[tuple[int, int, int, int]]

    def pixel(self, x: int, y: int) -> tuple[int, int, int, int]:
        return self.pixels[y * self.width + x]

    def palette(self) -> frozenset[tuple[int, int, int, int]]:
        return frozenset(self.pixels)


def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def _chunks(data: bytes):
    if not data.startswith(PNG_SIGNATURE):
        raise ValueError("not a PNG file")
    offset = len(PNG_SIGNATURE)
    while offset < len(data):
        if offset + 12 > len(data):
            raise ValueError("truncated PNG chunk")
        length = struct.unpack_from(">I", data, offset)[0]
        chunk_type = data[offset + 4:offset + 8]
        chunk_data = data[offset + 8:offset + 8 + length]
        crc_expected = struct.unpack_from(">I", data, offset + 8 + length)[0]
        crc_actual = zlib.crc32(chunk_type)
        crc_actual = zlib.crc32(chunk_data, crc_actual) & 0xFFFFFFFF
        if crc_actual != crc_expected:
            raise ValueError(f"CRC mismatch in {chunk_type!r}")
        yield chunk_type, chunk_data
        offset += 12 + length
        if chunk_type == b"IEND":
            return
    raise ValueError("PNG has no IEND chunk")


def read_png(data: bytes) -> PngImage:
    ihdr = None
    palette = None
    transparency = None
    compressed = bytearray()
    for chunk_type, chunk_data in _chunks(data):
        if chunk_type == b"IHDR":
            ihdr = struct.unpack(">IIBBBBB", chunk_data)
        elif chunk_type == b"PLTE":
            palette = [
                (chunk_data[i], chunk_data[i + 1], chunk_data[i + 2])
                for i in range(0, len(chunk_data), 3)
            ]
        elif chunk_type == b"tRNS":
            transparency = bytes(chunk_data)
        elif chunk_type == b"IDAT":
            compressed.extend(chunk_data)

    if ihdr is None:
        raise ValueError("PNG missing IHDR")
    width, height, bit_depth, color_type, compression, filter_method, interlace = ihdr
    allowed_depths = {
        0: {1, 2, 4, 8, 16},
        2: {8, 16},
        3: {1, 2, 4, 8},
        4: {8, 16},
        6: {8, 16},
    }
    if color_type not in allowed_depths or bit_depth not in allowed_depths[color_type]:
        raise ValueError(f"unsupported PNG color type {color_type}, bit depth {bit_depth}")
    if bit_depth == 16:
        raise ValueError("16-bit PNGs are not used by this project and are unsupported")
    if compression != 0 or filter_method != 0:
        raise ValueError("unsupported PNG compression/filter method")
    if interlace != 0:
        raise ValueError("interlaced PNGs are unsupported")

    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}.get(color_type)
    if channels is None:
        raise ValueError(f"unsupported PNG color type {color_type}")
    if color_type == 3 and palette is None:
        raise ValueError("indexed PNG missing PLTE")

    raw = zlib.decompress(bytes(compressed))
    bits_per_pixel = channels * bit_depth
    stride = (width * bits_per_pixel + 7) // 8
    filter_bpp = max(1, (bits_per_pixel + 7) // 8)
    expected = height * (stride + 1)
    if len(raw) != expected:
        raise ValueError(f"unexpected PNG scanline size {len(raw)} != {expected}")

    previous = bytearray(stride)
    rows: list[bytes] = []
    offset = 0
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        scanline = bytearray(raw[offset:offset + stride])
        offset += stride
        for i in range(stride):
            left = scanline[i - filter_bpp] if i >= filter_bpp else 0
            up = previous[i]
            upper_left = previous[i - filter_bpp] if i >= filter_bpp else 0
            if filter_type == 0:
                value = scanline[i]
            elif filter_type == 1:
                value = scanline[i] + left
            elif filter_type == 2:
                value = scanline[i] + up
            elif filter_type == 3:
                value = scanline[i] + ((left + up) // 2)
            elif filter_type == 4:
                value = scanline[i] + _paeth(left, up, upper_left)
            else:
                raise ValueError(f"unsupported PNG filter {filter_type}")
            scanline[i] = value & 0xFF
        rows.append(bytes(scanline))
        previous = scanline

    trns_gray = None
    trns_rgb = None
    if transparency is not None and color_type == 0 and len(transparency) >= 2:
        trns_gray = struct.unpack(">H", transparency[:2])[0]
    if transparency is not None and color_type == 2 and len(transparency) >= 6:
        trns_rgb = struct.unpack(">HHH", transparency[:6])

    def packed_sample(row: bytes, x: int, channel: int = 0) -> int:
        bit_index = (x * channels + channel) * bit_depth
        byte = row[bit_index // 8]
        shift = 8 - bit_depth - (bit_index % 8)
        return (byte >> shift) & ((1 << bit_depth) - 1)

    pixels: list[tuple[int, int, int, int]] = []
    for row in rows:
        for x in range(width):
            if bit_depth < 8:
                sample = packed_sample(row, x)
                if color_type == 0:
                    g = (sample * 255) // ((1 << bit_depth) - 1)
                    a = 0 if trns_gray == sample else 255
                    pixels.append((g, g, g, a))
                elif color_type == 3:
                    index = sample
                    if index >= len(palette):
                        raise ValueError(f"palette index {index} out of range")
                    r, g, b = palette[index]
                    a = transparency[index] if transparency is not None and index < len(transparency) else 255
                    pixels.append((r, g, b, a))
                else:
                    raise ValueError(f"packed samples unsupported for color type {color_type}")
                continue
            i = x * channels
            if color_type == 0:
                g = row[i]
                a = 0 if trns_gray == g else 255
                pixels.append((g, g, g, a))
            elif color_type == 2:
                r, g, b = row[i], row[i + 1], row[i + 2]
                a = 0 if trns_rgb == (r, g, b) else 255
                pixels.append((r, g, b, a))
            elif color_type == 3:
                index = row[i]
                if index >= len(palette):
                    raise ValueError(f"palette index {index} out of range")
                r, g, b = palette[index]
                a = transparency[index] if transparency is not None and index < len(transparency) else 255
                pixels.append((r, g, b, a))
            elif color_type == 4:
                g, a = row[i], row[i + 1]
                pixels.append((g, g, g, a))
            else:
                pixels.append((row[i], row[i + 1], row[i + 2], row[i + 3]))
    return PngImage(width, height, pixels)


def _chunk(chunk_type: bytes, data: bytes) -> bytes:
    crc = zlib.crc32(chunk_type)
    crc = zlib.crc32(data, crc) & 0xFFFFFFFF
    return struct.pack(">I", len(data)) + chunk_type + data + struct.pack(">I", crc)


def write_png(image: PngImage) -> bytes:
    if len(image.pixels) != image.width * image.height:
        raise ValueError("pixel count does not match image dimensions")
    scanlines = bytearray()
    for y in range(image.height):
        scanlines.append(0)  # Filter type: None. No interpolation or preprocessing.
        for x in range(image.width):
            scanlines.extend(image.pixel(x, y))
    ihdr = struct.pack(">IIBBBBB", image.width, image.height, 8, 6, 0, 0, 0)
    return (
        PNG_SIGNATURE
        + _chunk(b"IHDR", ihdr)
        + _chunk(b"IDAT", zlib.compress(bytes(scanlines), level=9))
        + _chunk(b"IEND", b"")
    )


def load_png(path) -> PngImage:
    return read_png(open(path, "rb").read())


def save_png(path, image: PngImage) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(write_png(image))


def make_image(width: int, height: int, color=(0, 0, 0, 0)) -> PngImage:
    return PngImage(width, height, [tuple(color) for _ in range(width * height)])


def set_pixel(image: PngImage, x: int, y: int, color) -> None:
    if not (0 <= x < image.width and 0 <= y < image.height):
        raise IndexError((x, y))
    image.pixels[y * image.width + x] = tuple(color)


def blit_scaled(dst: PngImage, src: PngImage, x0: int, y0: int, scale: int) -> None:
    if scale < 1:
        raise ValueError("scale must be positive")
    for y in range(src.height):
        for x in range(src.width):
            color = src.pixel(x, y)
            for dy in range(scale):
                for dx in range(scale):
                    set_pixel(dst, x0 + x * scale + dx, y0 + y * scale + dy, color)


def fill_rect(image: PngImage, x0: int, y0: int, width: int, height: int, color) -> None:
    for y in range(max(0, y0), min(image.height, y0 + height)):
        for x in range(max(0, x0), min(image.width, x0 + width)):
            set_pixel(image, x, y, color)


def draw_rect(image: PngImage, x0: int, y0: int, width: int, height: int, color) -> None:
    fill_rect(image, x0, y0, width, 1, color)
    fill_rect(image, x0, y0 + height - 1, width, 1, color)
    fill_rect(image, x0, y0, 1, height, color)
    fill_rect(image, x0 + width - 1, y0, 1, height, color)
