package com.minechai.create_candustry.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * 服务端上一次考古（刷掘）真正完成时触发的事件。
 *
 * <p>由两条互相独立的路径各触发一次：</p>
 * <ul>
 *     <li>玩家用原版刷子刷完可疑沙子 / 可疑沙砾 / 猫砂：{@code BrushableBlockEntity#brush} 返回 {@code true}；</li>
 *     <li>Create 机械手一次接触完成考古：{@code DeployerBrushCompat}。</li>
 * </ul>
 *
 * <p>事件只承载“应当计入进度的玩家”，用于把考古完成这一事实转发给可选兼容模块（例如 FTB Quests
 * 的自定义任务），从而让主代码不直接依赖这些模组。未安装对应模组时事件只是没有监听者。</p>
 */
public class ArchaeologyCompletedEvent extends Event {
    private final ServerPlayer player;

    public ArchaeologyCompletedEvent(ServerPlayer player) {
        this.player = player;
    }

    /** 本次考古应当计入进度的玩家（服务端）。 */
    public ServerPlayer getPlayer() {
        return player;
    }
}
