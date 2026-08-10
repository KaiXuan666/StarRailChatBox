# 本地模型来源与许可证

应用不在安装包、数据库导出或备份中包含模型文件。用户首次启用本地模式时自行下载，
或导入自己有权使用的 `.litertlm` 文件。

内置目录项：

- 模型：Qwen3 1.7B（LiteRT-LM 格式）
- 来源：https://huggingface.co/litert-community/Qwen3-1.7B
- 固定 revision：`d9b8a9126e5ac18591306eacd4311ba43b92421e`
- 文件：`Qwen3_1.7B.litertlm`
- SHA-256：`66064a4e9269cb693e124c4e3040bcb8a446b10bca42663896329495add3861c`
- 许可证：Apache License 2.0

手动导入模型的许可证与使用范围由用户负责确认。应用只将导入文件流式复制到应用私有
目录，不会上传模型内容。

Windows GPU 推理还需要 Microsoft DirectX Shader Compiler 的 `dxcompiler.dll` 与
`dxil.dll`。发行包应将这两个文件放在可执行文件同目录；开发环境缺失时应用会记录原因
并自动使用 CPU，不会阻断聊天。相关二进制来自 Microsoft
[DirectXShaderCompiler](https://github.com/microsoft/DirectXShaderCompiler/releases)，
应在发行流程中使用固定版本并保留其许可证文件。
