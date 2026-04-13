# FileLocator (FileHound Search)

A lightning-fast, standalone pure Java desktop search utility. Designed as a modern, zero-dependency alternative to classic tools like Locate32, FileLocator uses a custom binary indexing engine to provide instant search results across your local drives.

## 🌟 Key Features

* **Instant Search:** Find files by name, extension, or location instantly.
* **Advanced Filters:** Filter your results by exact file size ranges (KB/MB/GB) and modification date ranges.
* **Regex & Wildcards:** Support for standard globbing (`*.txt`, `report_??.pdf`) and full Regular Expressions.
* **Multi-Drive Scanning:** Scan specific folders, individual drives, or your entire PC ("This PC") at once.
* **Enterprise Safe (Zero Native DLLs):** Built entirely with standard Java libraries (Swing and NIO). Requires no SQLite or third-party native binaries, making it 100% compliant with strict enterprise security and App Control policies (e.g., Carbon Black).

## 🗄️ Architecture & Storage

To ensure speed and portability, FileLocator builds a highly optimized custom binary database (`.idx`) of your file system rather than relying on heavy SQL databases. 

The generated index file is safely stored in your user home directory to prevent read/write permission errors:
* **Windows:** `C:\Users\<YourUsername>\.file-search\files.idx`
* **macOS / Linux:** `~/.file-search/files.idx`

*Note: If you ever need to completely reset the application's memory or clear the database, you can safely close the application and delete the `.file-search` folder.*

## 🚀 Getting Started

### Prerequisites
* **Java Development Kit (JDK):** Version 17 or higher
* **Apache Maven:** For building the project

### Building the Project
1. Open your terminal or command prompt.
2. Navigate to the root directory of the project (where this `README.md` and the `pom.xml` are located).
3. Run the Maven clean package command to build the fat JAR:
   ```bash
   mvn clean package

