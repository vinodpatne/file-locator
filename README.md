# FileLocator (File & Folder Search)

A lightning-fast, standalone pure Java desktop search utility. Designed as a modern, zero-dependency alternative to classic search tools, FileLocator uses a highly optimized custom binary indexing engine to provide instant search results across your local drives.

---

## 🌟 Key Features

FileLocator is packed with powerful search features designed for speed, flexibility, and robust system integration:

### 1. High-Performance Index-Based Search
* **Instant Results:** Query search results instantly via an in-memory repository of your indexed drive structure.
* **Responsive Rendering:** Search queries are processed using Java concurrency (`parallelStream()`) and results are capped at 200 matches to ensure peak performance and rapid UI updates.
* **Persistent Cache:** The file system structure is saved locally in a highly compressed custom binary index file (`files.idx`), avoiding heavy relational databases.

### 2. Multi-Dimensional Search Criteria
* **Flexible Filename Matching:**
  * **Substring Matching:** Finds any file or folder containing the search term.
  * **Exact Match:** Wrap your search term in single (`'...'`) or double (`"..."`) quotes to find exact filename matches.
  * **Wildcards/Globs:** Use standard wildcards like `*` (matches zero or more characters) and `?` (matches any single character).
  * **Regular Expressions (Regex):** Select the Regex option to match files using full regular expression pattern syntax.
* **Extension Filtering:** Limit results to specific file types using a comma-separated list of extensions (e.g., `*.txt, .pdf, doc`).
* **Target Location & Scope:**
  * Scan individual folders or root drives.
  * Toggle **Search subdirectories** to switch between recursive search and top-level directory search.
  * Use **This PC** to execute scans or searches across all mounted local drives.
* **Size Filtering:** Restrict search results to specific size limits using Minimum and/or Maximum size constraints with size unit selectors (`KB`, `MB`, `GB`).
* **Date Modified Filtering:** Filter files by date modification ranges (newer than and/or older than) using date spinner components.

### 3. Advanced Tools & Interactive UI
* **Find Duplicates:** Locate duplicate files by grouping search results sharing the same name and size.
* **Smart Auto-Sort:** The UI automatically suggests sorting priorities based on the filters you use (e.g., sorting by size descending if size filters are active, or sorting by date descending if modification filters are active).
* **Manual Sorting:** Sort results dynamically by Name, Size, Date Modified, or File Path in Ascending or Descending order.
* **Dynamic Themes:** Toggle between clean **Light** and modern **Dark** themes instantly at runtime.

### 4. Interactive File Operations
* **Launch/Open Files:** Double-click a search result row, or click **Open** to open the selected file/folder using the OS-default application.
* **Show in Explorer (Open Location):** Click **Open Loc** to open the parent folder in Windows File Explorer with the selected file automatically highlighted.
* **Direct File Deletion:** Select files and press the `Delete` key (or click the **Delete** button) to delete them permanently from the disk. FileLocator automatically deletes directories recursively and updates the active search results and index.

---

## 🗄️ Architecture & Storage

To keep the application lightweight, fast, and fully portable, FileLocator avoids heavy database setups:
* **Custom Binary Index:** The app stores metadata in `files.idx`. This database holds absolute file paths, file sizes, folder flags, and last modified timestamps.
* **Atomic Index Swap:** Index building is written to a temporary file (`files.idx.tmp`) and swapped atomically using standard Java `ATOMIC_MOVE` operations (with fallback procedures) to prevent index corruption.
* **Smart Background Auto-Indexing:**
  * Monitors CPU load in the background. If utilization is low (under 15%) and the cooldown period has expired, it automatically updates the search index.
  * Protects system responsiveness by **throttling the indexer** if the active CPU usage exceeds 70%.
  * Automatically ignores heavy system folders (e.g., `C:\Windows`, `C:\Program Files`, `C:\$Recycle.Bin`, `C:\System Volume Information`) to optimize indexing duration.

---

## 🛠️ Security Compliance (Enterprise Safe)

FileLocator is built to run cleanly in high-security enterprise environments:
* **No Native DLLs:** FlatLaf native library loading is disabled (`flatlaf.useNativeLibrary = false`).
* **Zero Third-Party Binary Executables:** Runs on pure JVM bytecode with no local C++ or SQLite binaries.
* **App Control Friendly:** Easily passes strict application whitelist audits (e.g., Carbon Black, Windows Defender Application Control).

---

## ⚙️ Configuration & User Preferences

Application settings are saved inside the current execution directory in a file named `user-preferences.json`. It stores:
* Active UI theme preference (`Light` or `Dark`).
* Selected default scan directory.
* History of the last 10 searched folders for quick access in the "Look in" dropdown menu.

---

## 🚀 Getting Started

### Prerequisites
* **Java Development Kit (JDK):** Version 17 or higher.
* **Apache Maven:** For building the application.

### Building the Project
You can build the project by running:
```bash
mvn clean package
```

### Running the Application
Run the packaged JAR from the command line:
```bash
java -jar target/file-locator-1.0.1.jar
```
