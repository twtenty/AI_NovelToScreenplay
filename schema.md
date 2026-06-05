# 剧本 YAML Schema（简版）

完整 Schema 说明与设计原因见 [`YAML_SCHEMA.md`](YAML_SCHEMA.md)。

核心结构如下：

```yaml
title: 小说名称
source: 原文来源
generation_mode: ai
chapters:
  - chapter: 第一章
    summary: 章节概述
    scenes:
      - scene_id: "1.1"
        title: 场景标题
        location: 场景地点
        time: 场景时间
        description: 场景概述与氛围
        characters:
          - 角色名
        actions:
          - actor: 角色名
            text: 动作或行为描述
        dialogues:
          - speaker: 角色名
            line: 角色对白
        notes: 改编建议或后续修改说明
```
