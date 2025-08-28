<h1 align="center">Logger++ Collector</h1>
<h4 align="center">Advanced Logging for Burp Suite with HTTP Traffic Collection</h4>
<p align="center">
  
  <img src="https://img.shields.io/github/watchers/nccgroup/LoggerPlusPlus?label=Watchers&style=for-the-badge" alt="GitHub Watchers">
  <img src="https://img.shields.io/github/stars/nccgroup/LoggerPlusPlus?style=for-the-badge" alt="GitHub Stars">
  <img src="https://img.shields.io/github/downloads/nccgroup/LoggerPlusPlus/total?style=for-the-badge" alt="GitHub All Releases">
  <img src="https://img.shields.io/github/license/nccgroup/LoggerPlusPlus?style=for-the-badge" alt="GitHub License">
</p>
<p align="right">
  <b>English</b> | <a href="./README_CH.md">中文版</a>
</p>

Developed by Corey
Arthur  [![Twitter Follow](https://img.shields.io/badge/follow-%40CoreyD97-1DA1F2?logo=twitter&style=social)](https://twitter.com/coreyd97/)  
Originally by Soroush
Dalili  [![Twitter Follow](https://img.shields.io/badge/follow-%40irsdl-1DA1F2?logo=twitter&style=social)](https://twitter.com/irsdl/)

Released as open source by NCC Group Plc - https://www.nccgroup.com/  
Released under AGPL-3.0 see LICENSE for more information

Description
----

Logger++ Collector is a modified version of the Logger++ extension for Burp Suite. In addition to all the original Logger++ features, this version adds the ability to automatically push captured HTTP traffic (requests and responses) to a custom backend service for archiving, threat detection, or AI analysis.

Logger++ is a multithreaded logging extension for Burp Suite. In addition to logging requests and responses from all
Burp Suite tools, the extension allows advanced filters to be defined to highlight interesting entries or filter logs to
only those which match the filter.

A built in grep tool allows the logs to be searched to locate entries which match a specified pattern, and extract the
values of the capture groups.

To enable logs to be used in other systems, the table can also be uploaded to elasticsearch or exported to CSV.

<b>Features:</b>

- Works with the latest version of Burp Suite (tested on 1.7.27)
- Logs all the tools that are sending requests and receiving responses
- Ability to log from a specific tool
- Ability to save the results in CSV format
- Ability to show results of custom regular expressions in request/response
- User can customise the column headers
- Advanced Filters can be created to display only requests matching a specific string or regex pattern.
- Row highlighting can be added using advanced filters to make interesting requests more visible.
- Grep through logs.
- Live requests and responses.
- Multiple view options.
- Pop out view panel.
- Multithreaded.
- **NEW: Collector feature to push HTTP traffic to custom backend services**
- **NEW: Domain whitelist filtering for traffic collection**
- **NEW: Configurable backend integration with authentication**

<b>Current Limitations:</b>

- Cannot log the requests' actual time unless originating from proxy tool.
- Cannot calculate the actual delay between a request and its response unless originating from proxy tool.

Screenshots
----------------------

<b>Log Filters</b>

![Log Filters](images/filters.png)

<b>Row Highlights</b>

![Row Highlights](images/colorfilters.png)

<b>Grep Search</b>

![Grep Panel](images/grep.png)


Usage
----
You can use this extension without using the BApp store. In order to install the latest version of this extension from
the GitHub repository, follow these steps:

1. Download the latest release jar file `loggerplusplus-collector.jar`.

2. In Burp Suite, click on the "Extender" tab, then in the "Extensions" tab click on the "Add" button and select the
   downloaded "loggerplusplus-collector.jar" file.

3. You should now be able to see the "Logger++" tab in Burp Suite. If it cannot log anything, check your Burp Suite
   extension settings. If the save buttons are disabled, make sure that the requested libraries have been loaded
   successfully; Unload and then reload the extension and try again. If you have found an issue, please report it in the
   GitHub project.

4. You can configure this extension by using its "option" tab and by right click on the columns' headers.

5. To configure the Collector feature, click on the "Collector" tab and set up the following parameters:
   - Enable Collector: Toggle to enable/disable the traffic collection feature
   - Server URL: The URL to send HTTP traffic to (e.g., http://127.0.0.1:5000/recv)
   - Subsystem Name: Name to identify this subsystem
   - Domain Whitelist: Enter domains to whitelist, one per line (leave empty to collect all traffic)
   - Secret Key: Secret key for authentication with the server

6. Click "Test Connection" to verify connectivity with your backend server.

7. Click "Save Configuration" to save your settings.

8. If you like the project, give the repo a star! <3

![Stargazers](https://starchart.cc/nccgroup/LoggerPlusPlus.svg)

Python Backend Server
----

A simple Python Flask backend server is provided to receive and display HTTP traffic sent by the Logger++ Collector. To use it:

1. Make sure you have Python and Flask installed: `pip install flask`
2. Save the following script as `collector_backend.py`:

```python
from flask import Flask, request, jsonify, render_template_string
import json
import os
import time
import base64

app = Flask(__name__)

# Configuration
STORAGE_DIR = "collected_traffic"
SECRET_KEY = "your_secret_key"  # Change this to match your Collector configuration
VERIFY_SECRET = True  # Set to False to disable secret key verification

# Create storage directory if it doesn't exist
os.makedirs(STORAGE_DIR, exist_ok=True)

# HTML template for the list page
LIST_TEMPLATE = '''
<!DOCTYPE html>
<html>
<head>
    <title>Collected Traffic</title>
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
    <h1>Collected Traffic</h1>
    <ul>
    {% for file in files %}
        <li><a href="/view/{{ file }}">{{ file }}</a></li>
    {% endfor %}
    </ul>
</body>
</html>
'''

# HTML template for the view page
VIEW_TEMPLATE = '''
<!DOCTYPE html>
<html>
<head>
    <title>Traffic Details</title>
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
    <a href="/list">← Back to list</a>
    <h1>Traffic Details</h1>
    <h2>Metadata</h2>
    <pre class="metadata">{{ metadata }}</pre>
    <h2>Request</h2>
    <pre class="request">{{ request }}</pre>
    <h2>Response</h2>
    <pre class="response">{{ response }}</pre>
</body>
</html>
'''

@app.route("/recv", methods=["POST"])
def receive_traffic():
    try:
        data = request.json
        
        # Verify secret key if enabled
        if VERIFY_SECRET:
            if "secret" not in data or data["secret"] != SECRET_KEY:
                return jsonify({"status": "error", "message": "Invalid secret key"}), 403
        
        # Extract data
        subsystem = data.get("subsystem", "unknown")
        host = data.get("host", "unknown")
        req = data.get("request", "")
        resp = data.get("response", "")
        
        # Create filename with timestamp
        timestamp = int(time.time())
        filename = f"{timestamp}_{subsystem}_{host}.json"
        filepath = os.path.join(STORAGE_DIR, filename)
        
        # Save to file
        with open(filepath, "w") as f:
            json.dump(data, f, indent=2)
        
        return jsonify({"status": "success", "message": "Traffic received and saved"})
    
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route("/list")
def list_files():
    files = os.listdir(STORAGE_DIR)
    files.sort(reverse=True)  # Show newest first
    return render_template_string(LIST_TEMPLATE, files=files)

@app.route("/view/<filename>")
def view_file(filename):
    try:
        filepath = os.path.join(STORAGE_DIR, filename)
        with open(filepath, "r") as f:
            data = json.load(f)
        
        # Prepare metadata
        metadata = {
            "subsystem": data.get("subsystem", "unknown"),
            "host": data.get("host", "unknown"),
            "timestamp": filename.split("_")[0]
        }
        
        # Get request and response
        request_data = data.get("request", "")
        response_data = data.get("response", "")
        
        return render_template_string(
            VIEW_TEMPLATE,
            metadata=json.dumps(metadata, indent=2),
            request=request_data,
            response=response_data
        )
    
    except Exception as e:
        return f"Error: {str(e)}", 500

if __name__ == "__main__":
    print(f"Collector backend server running on http://127.0.0.1:5000")
    print(f"Storage directory: {os.path.abspath(STORAGE_DIR)}")
    print(f"Secret key verification: {'Enabled' if VERIFY_SECRET else 'Disabled'}")
    app.run(debug=True)
```

3. Run the server: `python collector_backend.py`
4. Configure the Logger++ Collector tab with:
   - Server URL: `http://127.0.0.1:5000/recv`
   - Secret Key: `your_secret_key` (or change in the script)
5. Access the collected traffic at `http://127.0.0.1:5000/list`

Contributing
----

### Building from Source

If you'd like to build the project from source, the project uses Gradle to simplify the process:

1. Clone the repository
2. Within the project folder, execute `gradlew jar` (Linux) `gradlew.bat jar` (Windows)
3. Once complete, you can find the built Jar in the project's `releases` folder.

### Testing

IntelliJ Idea has been used to develop the project, but feel free to use any IDE you prefer. The following instructions
are for Idea, but the process will be similar for other IDEs.

1. Within Idea, click `File > Open`, then select the project's `build.gradle` file.
2. Click "Open as Project" in the next dialog.
3. To run/debug the application, navigate to `Run > Edit Configurations`, then add a new "Application" configuration
   targeting the `TestLogger` class.

### Reporting bugs

If you have found an issue, please report it via [GitHub](https://github.com/nccgroup/LoggerPlusPlus/issues/new/choose).
