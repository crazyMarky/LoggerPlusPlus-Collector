#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from flask import Flask, request, jsonify
import json
import datetime
import os

app = Flask(__name__)

# 配置
SECRET_KEY = ""  # 可以设置一个密钥，为空则不验证
STORAGE_DIR = "collected_traffic"  # 存储目录

# 确保存储目录存在
if not os.path.exists(STORAGE_DIR):
    os.makedirs(STORAGE_DIR)

@app.route('/test', methods=['POST'])
def test_connection():
    # 简单的连接测试接口，不需要验证字段
    return jsonify({"status": "success", "message": "Connection test successful"})

@app.route('/')
def index():
    return '''
    <html>
        <head>
            <title>Logger++ Collector Backend</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                h1 { color: #333; }
                .container { max-width: 800px; margin: 0 auto; }
                .info { background-color: #f0f0f0; padding: 15px; border-radius: 5px; }
                pre { background-color: #f8f8f8; padding: 10px; border-radius: 5px; overflow: auto; }
                .endpoint { font-weight: bold; color: #0066cc; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>Logger++ Collector Backend</h1>
                <div class="info">
                    <p>这是一个简单的HTTP流量收集后端，用于接收BurpSuite Logger++ Collector插件发送的流量。</p>
                    <p>接收端点: <span class="endpoint">/recv</span></p>
                    <p>测试连接端点: <span class="endpoint">/test</span></p>
                    <p>查看已收集的流量: <a href="/list">/list</a></p>
                </div>
            </div>
        </body>
    </html>
    '''

@app.route('/recv', methods=['POST', 'HEAD'])
def receive_data():
    # 如果是HEAD请求，直接返回200状态码
    if request.method == 'HEAD':
        return "", 200
    try:
        data = request.get_json()
        
        # 验证必要字段
        required_fields = ['subsystem', 'host', 'request', 'response']
        for field in required_fields:
            if field not in data:
                return jsonify({"status": "error", "message": f"Missing required field: {field}"}), 400
        
        # 验证密钥（如果设置了）
        if SECRET_KEY and data.get('secret') != SECRET_KEY:
            return jsonify({"status": "error", "message": "Invalid secret key"}), 403
        
        # 保存数据
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S_%f")
        filename = f"{timestamp}_{data['subsystem']}_{data['host']}.json"
        filepath = os.path.join(STORAGE_DIR, filename)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        return jsonify({"status": "success", "message": "Data received and saved"})
    
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/list')
def list_files():
    files = [f for f in os.listdir(STORAGE_DIR) if f.endswith('.json')]
    files.sort(reverse=True)  # 最新的文件排在前面
    
    html = '''
    <html>
        <head>
            <title>Collected Traffic</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                h1 { color: #333; }
                .container { max-width: 1000px; margin: 0 auto; }
                table { width: 100%; border-collapse: collapse; }
                th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
                th { background-color: #f2f2f2; }
                tr:hover { background-color: #f5f5f5; }
                .view-link { color: #0066cc; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>已收集的HTTP流量</h1>
                <p><a href="/">返回首页</a></p>
                <table>
                    <tr>
                        <th>时间戳</th>
                        <th>子系统</th>
                        <th>主机</th>
                        <th>操作</th>
                    </tr>
    '''
    
    for file in files:
        parts = file.split('_', 3)
        if len(parts) >= 4:
            date = parts[0]
            time = parts[1]
            subsystem = parts[2]
            host = parts[3].replace('.json', '')
            
            html += f'''
                    <tr>
                        <td>{date} {time}</td>
                        <td>{subsystem}</td>
                        <td>{host}</td>
                        <td><a class="view-link" href="/view/{file}">查看</a></td>
                    </tr>
            '''
    
    html += '''
                </table>
            </div>
        </body>
    </html>
    '''
    
    return html

@app.route('/view/<filename>')
def view_file(filename):
    filepath = os.path.join(STORAGE_DIR, filename)
    
    if not os.path.exists(filepath):
        return "File not found", 404
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # 格式化显示
        html = f'''
        <html>
            <head>
                <title>Traffic Details</title>
                <style>
                    body {{ font-family: Arial, sans-serif; margin: 20px; }}
                    h1, h2 {{ color: #333; }}
                    .container {{ max-width: 1200px; margin: 0 auto; }}
                    .info {{ background-color: #f0f0f0; padding: 15px; border-radius: 5px; margin-bottom: 20px; }}
                    pre {{ background-color: #f8f8f8; padding: 10px; border-radius: 5px; overflow: auto; white-space: pre-wrap; }}
                    .nav {{ margin-bottom: 20px; }}
                    .request {{ border-left: 4px solid #4CAF50; padding-left: 10px; }}
                    .response {{ border-left: 4px solid #2196F3; padding-left: 10px; }}
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="nav">
                        <a href="/list">返回列表</a>
                    </div>
                    <h1>HTTP流量详情</h1>
                    <div class="info">
                        <p><strong>子系统:</strong> {data.get('subsystem', 'N/A')}</p>
                        <p><strong>主机:</strong> {data.get('host', 'N/A')}</p>
                    </div>
                    
                    <h2>请求</h2>
                    <pre class="request">{data.get('request', 'N/A')}</pre>
                    
                    <h2>响应</h2>
                    <pre class="response">{data.get('response', 'N/A')}</pre>
                </div>
            </body>
        </html>
        '''
        
        return html
    
    except Exception as e:
        return f"Error: {str(e)}", 500

if __name__ == '__main__':
    print("Logger++ Collector Backend 启动中...")
    print(f"接收端点: http://127.0.0.1:5000/recv")
    print(f"Web界面: http://127.0.0.1:5000/")
    print(f"存储目录: {os.path.abspath(STORAGE_DIR)}")
    app.run(debug=True, host='127.0.0.1', port=5000)