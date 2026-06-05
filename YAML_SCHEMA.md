# 剧本 YAML Schema

## 目标

该 Schema 用于把 3 个章节以上的小说文本转换为可编辑、可扩展、便于二次创作的剧本初稿。它重点解决三个问题：保留小说章节结构、把叙事文本改写为场景化表达、让作者能快速定位并修改对白和动作。

## Schema 示例

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

## 字段说明

- `title`：剧本名称，通常来自小说标题。
- `source`：小说原文或章节来源，用于记录输入范围。
- `generation_mode`：生成方式，例如 `ai` 或 `demo_without_api_key`。
- `chapters`：章节数组，保留小说原有章节边界。
- `chapter`：章节名称或编号。
- `summary`：章节整体概要，帮助作者快速回看剧情。
- `scenes`：章节内的场景列表。
- `scene_id`：场景编号，例如 `1.1` 表示第一章第一场。
- `title`：场景标题，用于概括该场核心事件。
- `location`：场景发生地点。
- `time`：场景时间，例如“清晨”“夜晚”“会议结束后”。
- `description`：场景描述，包含环境、氛围、冲突和情节推进。
- `characters`：本场景出现的角色名单。
- `actions`：动作列表，用 `actor` 和 `text` 区分执行者与动作内容。
- `dialogues`：对白列表，用 `speaker` 和 `line` 区分说话者与台词。
- `notes`：改编建议、情绪提示或需要作者进一步打磨的内容。

## 设计原因

1. **保留章节结构**

小说作者通常按章节组织作品。Schema 保留 `chapters`，可以让作者在改编后仍然按原文脉络检查剧情，不会在一次生成中失去整体结构。

2. **以场景作为剧本核心单位**

剧本的基本推进单位是场景，而不是自然段。`scenes` 将小说叙述拆成可拍摄、可表演、可修改的片段，方便后续扩展到分镜、拍摄计划或舞台调度。

3. **分离动作与对白**

小说经常把心理、动作、对白混写在一起。Schema 将 `actions` 和 `dialogues` 拆开，使剧本更接近真实创作格式，也便于作者单独修改台词或动作。

4. **便于自动生成与程序处理**

YAML 可读性强，层级清晰，适合 LLM 直接生成，也方便后续增加校验脚本、编辑器表单、导出 PDF 或转换为其他剧本格式。
