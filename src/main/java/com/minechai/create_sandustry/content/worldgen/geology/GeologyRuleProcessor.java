package com.minechai.create_sandustry.content.worldgen.geology;

/**
 * 可插拔地质规则。新增规则只需实现本接口并登记到 {@link GeologyRules}。
 */
public interface GeologyRuleProcessor {
    String id();

    boolean isEnabled(GeologyConfigSnapshot config);

    boolean apply(GeologyGenerationContext context, GeologyConfigSnapshot config);
}
