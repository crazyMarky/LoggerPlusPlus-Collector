<h1 align="center">Logger++ Collector</h1>
<h4 align="center">Burp Suite 高级日志扩展，支持 HTTP 流量收集与上报</h4>
<p align="center">

  <img src="https://img.shields.io/github/watchers/nccgroup/LoggerPlusPlus?label=Watchers&style=for-the-badge" alt="GitHub Watchers">
  <img src="https://img.shields.io/github/stars/nccgroup/LoggerPlusPlus?style=for-the-badge" alt="GitHub Stars">
  <img src="https://img.shields.io/github/downloads/nccgroup/LoggerPlusPlus/total?style=for-the-badge" alt="GitHub All Releases">
  <img src="https://img.shields.io/github/license/nccgroup/LoggerPlusPlus?style=for-the-badge" alt="GitHub License">
</p>
<p align="right">
  <a href="./README.md">English</a> | <b>中文版</b>
</p>

由 Corey Arthur 开发 [![Twitter Follow](https://img.shields.io/badge/follow-%40CoreyD97-1DA1F2?logo=twitter&style=social)](https://twitter.com/coreyd97/)  
原作者 Soroush Dalili [![Twitter Follow](https://img.shields.io/badge/follow-%40irsdl-1DA1F2?logo=twitter&style=social)](https://twitter.com/irsdl/)

由 NCC Group Plc 开源发布 - https://www.nccgroup.com/  
开源协议：AGPL-3.0，详情见 LICENSE

---

## 简介

**Logger++ Collector** 是基于 **Logger++** 扩展的增强版本。  
在保留原始 Logger++ 全部功能的基础上，新增了 **自动将捕获的 HTTP 请求与响应推送至后端服务** 的能力，用于日志归档、安全检测或 AI 分析。

Logger++ 本身是一个 Burp Suite 多线程日志扩展：
- 支持记录 Burp 所有工具的请求和响应
- 支持自定义高级过滤规则，突出显示或筛选关键流量
- 内置 Grep 工具，可快速搜索日志并提取内容
- 可将日志上传至 Elasticsearch 或导出为 CSV

**Collector 增强功能** 则进一步实现了自动化与系统集成。

---

## 特性

- ✅ 支持最新版 Burp Suite（测试于 1.7.27）
- ✅ 记录所有 Burp 工具产生的请求/响应
- ✅ 支持单独选择记录某个工具流量
- ✅ 支持保存结果为 CSV
- ✅ 支持正则匹配提取请求/响应内容
- ✅ 自定义列头显示
- ✅ 高级过滤与行高亮，突出显示重要请求
- ✅ 内置 Grep 搜索
- ✅ 实时展示请求与响应
- ✅ 多种视图模式 & 弹出面板
- ✅ 多线程优化，性能更好
- 🌟 **新功能：Collector 模块，自动推送流量到自定义后端服务**
- 🌟 **新功能：支持域名白名单过滤，避免无关流量上报**
- 🌟 **新功能：后端配置与鉴权（Secret Key）支持**

---

## 当前限制

- 无法精确记录非代理流量的实际请求时间
- 无法计算请求与响应的真实延迟（仅限 Proxy 工具可用）

---

## 截图

**日志过滤器**

![Log Filters](images/filters.png)

**行高亮**

![Row Highlights](images/colorfilters.png)

**Grep 搜索**

![Grep Panel](images/grep.png)

---

## 使用方法

你可以直接从 GitHub Release 下载并手动加载扩展（无需 BApp 商店）。

1. 下载最新的 `loggerplusplus-collector.jar`
2. 在 Burp Suite 中，进入 **Extender → Extensions → Add**，选择该 Jar 文件
3. 加载后，你会看到 **Logger++ 标签页**
4. 如果无法正常记录，请检查扩展依赖是否加载，尝试卸载后重新加载
5. 通过 **Options 标签页** 可配置显示参数
6. **Collector 配置方法**：
    - 打开 **Collector 标签页**
    - 设置以下参数：
        - **Enable Collector**：启用/禁用流量上报
        - **Server URL**：后端接收地址（如 `http://127.0.0.1:5000/recv`）
        - **Subsystem Name**：子系统标识
        - **Domain Whitelist**：仅推送指定域名流量（每行一个，留空表示全部推送）
        - **Secret Key**：服务端鉴权密钥
    - 点击 **Test Connection** 测试连接
    - 点击 **Save Configuration** 保存配置

---

## Python 后端示例

我们提供了一个简单的 **Flask 后端示例**，可接收并展示 Collector 推送的流量。

1. 安装依赖：
   ```bash
   pip install flask


```python
from flask import Flask, request, jsonify, render_template_string
import json
import os
import time
import base64

app = Flask(__name__)

# 配置
STORAGE_DIR = "collected_traffic" # 存储目录
SECRET_KEY = "your_secret_key"  # 请修改此密钥，使其与收集器配置中的密钥匹配
VERIFY_SECRET = True  # 设置为 False 可禁用密钥验证

# 如果存储目录不存在，则创建它
os.makedirs(STORAGE_DIR, exist_ok=True)

# 列表页面的 HTML 模板
LIST_TEMPLATE = '''
<!DOCTYPE html>
<html>
<head>
    <title>收集到的流量</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        ul { list-style-type: none; padding: 0; }
        li { margin: 10px 0; padding: 10px; background-color: #f5f5f5; border-radius: 5px; }
        a { color: #0066cc; text-decoration: none; }
        a:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <h1>收集到的流量</h1>
    <ul>
    {% for file in files %}
        <li><a href="/view/{{ file }}">{{ file }}</a></li>
    {% endfor %}
    </ul>
</body>
</html>
'''

# 详情页面的 HTML 模板
VIEW_TEMPLATE = '''
<!DOCTYPE html>
<html>
<head>
    <title>流量详情</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1, h2 { color: #333; }
        pre { background-color: #f5f5f5; padding: 10px; border-radius: 5px; overflow-x: auto; }
        .request { border-left: 4px solid #4CAF50; }
        .response { border-left: 4px solid #2196F3; }
        .metadata { border-left: 4px solid #FF9800; }
        a { color: #0066cc; text-decoration: none; margin-bottom: 20px; display: inline-block; }
        a:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <a href="/list">← 返回列表</a>
    <h1>流量详情</h1>
    <h2>元数据</h2>
    <pre class="metadata">{{ metadata }}</pre>
    <h2>请求</h2>
    <pre class="request">{{ request }}</pre>
    <h2>响应</h2>
    <pre class="response">{{ response }}</pre>
</body>
</html>
'''

@app.route("/recv", methods=["POST"])
def receive_traffic():
    try:
        data = request.json
        
        # 如果启用，则验证密钥
        if VERIFY_SECRET:
            if "secret" not in data or data["secret"] != SECRET_KEY:
                return jsonify({"status": "error", "message": "无效的密钥"}), 403
        
        # 提取数据
        subsystem = data.get("subsystem", "unknown")
        host = data.get("host", "unknown")
        req = data.get("request", "")
        resp = data.get("response", "")
        
        # 使用时间戳创建文件名
        timestamp = int(time.time())
        filename = f"{timestamp}_{subsystem}_{host}.json"
        filepath = os.path.join(STORAGE_DIR, filename)
        
        # 保存到文件
        with open(filepath, "w") as f:
            json.dump(data, f, indent=2)
        
        return jsonify({"status": "success", "message": "流量已接收并保存"})
    
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route("/list")
def list_files():
    files = os.listdir(STORAGE_DIR)
    files.sort(reverse=True)  # 最新文件排在最前
    return render_template_string(LIST_TEMPLATE, files=files)

@app.route("/view/<filename>")
def view_file(filename):
    try:
        filepath = os.path.join(STORAGE_DIR, filename)
        with open(filepath, "r") as f:
            data = json.load(f)
        
        # 准备元数据
        metadata = {
            "subsystem": data.get("subsystem", "unknown"),
            "host": data.get("host", "unknown"),
            "timestamp": filename.split("_")[0]
        }
        
        # 获取请求和响应数据
        request_data = data.get("request", "")
        response_data = data.get("response", "")
        
        return render_template_string(
            VIEW_TEMPLATE,
            metadata=json.dumps(metadata, indent=2, ensure_ascii=False),
            request=request_data,
            response=response_data
        )
    
    except Exception as e:
        return f"错误: {str(e)}", 500

if __name__ == "__main__":
    print(f"收集器后端服务器正在 http://127.0.0.1:5000 运行")
    print(f"数据存储目录: {os.path.abspath(STORAGE_DIR)}")
    print(f"密钥验证: {'已启用' if VERIFY_SECRET else '已禁用'}")
    app.run(debug=True)
```
