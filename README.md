# ADVANCED-CODE-PROCESSOR

**Hackathon Project – 750-line Java CLI Tool**

The **Advanced Code Processor** is a Java-based Command-Line Interface (CLI) tool designed to **clean, document, and analyze multi-language codebases**.  
It is lightweight yet powerful, making it ideal for **hackathons, refactoring tasks, and developer productivity.**

---

## 🔑 Key Features
- 📊 **Code Metrics** – Line counts, variable usage, complexity analysis.  
- ✂️ **Duplicate & Blank Line Removal** – Cleans redundant code.  
- 🔍 **Search & Regex Filtering** – Process only files or methods you care about.  
- 🛠 **Editing & Refactoring** – Rename variables/methods, transform case, format indentation.  
- 📂 **Multi-language Support** – Works on `.java`, `.py`, `.cpp`, `.js`.  
- 📑 **Documentation Auto-generation** – Adds comments/Javadoc automatically.  
- 📜 **Reporting** – Detailed report (`processing_report.txt`) with file metrics.  
- 📝 **Error Logging** – Logs all issues for debugging.  

---

## ⚙️ How It Works
1. User provides a **directory path** with source files.  
2. The tool scans all supported files (`.java`, `.py`, `.cpp`, `.js`).  
3. It cleans, restructures, and documents code based on user options.  
4. Generates:
   - `processed_code/` → Cleaned code files  
   - `processing_report.txt` → Detailed metrics  
   - `error_log.txt` → Debug logs  

---

## 🖥️ Demo
Run in terminal:
```bash
javac AdvancedCodeProcessor.java
java AdvancedCodeProcessor
