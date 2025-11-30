# 国际化(i18n)文本使用规范

## 1. 概述

本项目采用JavaFX的ResourceBundle机制实现国际化支持，允许应用程序在不同语言环境下显示相应的文本内容。当前默认语言为中文(zh-CN)，同时提供英文作为fallback语言。

## 2. 资源文件结构

国际化资源文件位于`src/main/resources/i18n/`目录下，采用以下命名规范：

```
messages.properties          # 默认语言包（英文）
messages_zh_CN.properties    # 中文语言包
messages_en_US.properties    # 美式英语语言包（示例）
messages_ja_JP.properties    # 日语语言包（示例）
```

## 3. 资源文件格式

资源文件采用键值对格式，每行一个条目，格式为：

```
key=value
```

### 3.1 键命名规范

- 使用点分隔的层级结构，例如：`menu.file.open`
- 键名应具有描述性，清晰表达文本的用途和位置
- 键名使用小写字母和下划线，避免使用空格和特殊字符
- 按功能模块组织键名，例如：
  - `menu.` 前缀用于菜单栏文本
  - `toolbar.` 前缀用于工具栏按钮文本
  - `label.` 前缀用于标签文本
  - `dialog.` 前缀用于对话框文本

### 3.2 示例

```properties
# 菜单栏
menu.file=文件
menu.edit=编辑
menu.view=视图
menu.help=帮助

# 文件菜单
menu.file.open=打开...
menu.file.save=保存...
```

## 4. 在代码中使用国际化文本

### 4.1 初始化国际化

国际化工具类会在应用启动时自动初始化，默认使用中文语言环境。可以通过以下方式手动初始化或切换语言：

```java
// 初始化国际化资源
I18nUtil.initialize(Locale.CHINA);

// 切换到中文
I18nUtil.switchToChinese();

// 切换到英文
I18nUtil.switchToEnglish();
```

### 4.2 获取国际化文本

使用`I18nUtil.getString()`方法获取国际化文本：

```java
// 获取简单文本
String title = I18nUtil.getString("menu.file.open");

// 获取带占位符的文本
String errorMsg = I18nUtil.getString("dialog.error.fileOpenFailed", fileName);
```

### 4.3 在JavaFX控制器中使用

1. 在FXML文件中定义控件，但不设置文本属性
2. 在控制器中注入控件
3. 在`initialize()`方法中使用国际化工具设置文本

```java
@FXML
private Label luaCodeLabel;

@FXML
public void initialize() {
    luaCodeLabel.setText(I18nUtil.getString("label.luaCode"));
}
```

### 4.4 在文本编辑器中使用

对于动态生成的文本，直接使用国际化工具获取文本：

```java
codeArea.replaceText(I18nUtil.getString("editor.welcome"));
```

## 5. 添加新的语言支持

要添加新的语言支持，只需按照以下步骤操作：

1. 在`src/main/resources/i18n/`目录下创建新的语言包文件，命名格式为`messages_语言代码_国家代码.properties`
2. 复制`messages.properties`文件的内容到新文件中
3. 将所有值翻译为目标语言
4. 确保新语言包包含所有键，或者依赖默认语言包作为fallback

## 6. 最佳实践和注意事项

1. **避免硬编码文本**：所有用户可见的文本都应通过国际化工具获取，避免在代码中硬编码
2. **保持键的一致性**：确保所有语言包中的键名保持一致
3. **使用描述性键名**：键名应清晰表达文本的用途和位置，便于维护
4. **使用占位符处理动态文本**：对于包含变量的文本，使用`{0}`、`{1}`等占位符，避免字符串拼接
5. **定期检查缺失的键**：在添加新功能时，确保为所有新文本添加对应的国际化键
6. **测试不同语言环境**：在添加或修改语言包后，测试应用在不同语言环境下的显示效果

## 7. 国际化工具类

项目提供了`I18nUtil`工具类，封装了国际化相关的功能：

- `initialize(Locale locale)`：初始化国际化资源
- `getString(String key)`：获取国际化文本
- `getString(String key, Object... args)`：获取带占位符的国际化文本
- `switchLocale(Locale locale)`：切换语言环境
- `switchToChinese()`：切换到中文
- `switchToEnglish()`：切换到英文

## 8. 故障排除

1. **文本显示为键名**：检查资源文件中是否存在对应的键，或者键名是否拼写错误
2. **语言切换不生效**：确保在切换语言后，重新设置所有控件的文本
3. **特殊字符显示异常**：确保资源文件使用UTF-8编码

## 9. 扩展建议

1. 可以添加语言切换菜单，允许用户在运行时切换语言
2. 可以实现自动检测系统语言的功能
3. 可以添加更多语言支持，如西班牙语、法语、德语等

## 10. 总结

本项目的国际化功能采用了灵活的设计，便于扩展和维护。遵循上述规范，可以确保应用程序在不同语言环境下都能提供良好的用户体验。