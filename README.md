# 💰 Daily Expense & Income Tracker

A **Java desktop application** for tracking personal finances — both expenses and income — featuring a modern Swing GUI and a CLI fallback mode. Built with clean MVC architecture, it supports income/expense categorization, monthly budgeting, visual analytics, CSV export, and persistent local storage.

---

## ✨ Features

- 💚 **Income Tracking** — Log income with source categories (Salary, Freelance, Scholarship, Gift, Investment, Rental, Other)
- 📊 **Dashboard** — 4 KPI cards: Total Income, Total Expenses, Net Balance (Income − Expenses), and Budget Status
- 📋 **Transactions** — Unified view of all income and expense entries; filter by type (All / Income / Expenses), date, category, or keyword; color-coded rows
- 📈 **Analytics & Charts** — Expense category donut chart + Monthly Income vs Expenses grouped bar chart (last 6 months)
- 💰 **Budget & Alerts** — Set monthly budget limits; get color-coded warnings when approaching or exceeding limits
- ⚙️ **Currency & Settings** — Switch between multiple currencies (USD, EUR, GBP, INR, JPY, CAD, and more)
- 💾 **Persistent Storage** — All data saved locally as JSON via `StorageManager` (fully backward-compatible)
- 📤 **CSV Export** — Export filtered transactions to CSV
- 🖥️ **Dual Mode** — Runs as a GUI (Swing) app by default, or as a CLI app via `--cli` flag

---

## 🗂️ Project Structure

```
Daily Expense Tracker/
├── src/
│   └── com/expensetracker/
│       ├── Main.java                   # Application entry point
│       ├── model/
│       │   ├── Expense.java            # Expense data model
│       │   └── ExpenseCategory.java    # Category enum (Food, Transport, etc.)
│       ├── service/
│       │   └── ExpenseService.java     # Business logic (CRUD, budget, filtering)
│       ├── storage/
│       │   ├── StorageManager.java     # JSON-based local persistence
│       │   └── CsvExporter.java        # CSV export utility
│       ├── ui/
│       │   ├── MainFrame.java          # Main window with sidebar navigation
│       │   ├── DashboardPanel.java     # Dashboard view
│       │   ├── ExpenseTablePanel.java  # Expense table view
│       │   ├── ExpenseDialog.java      # Add/Edit expense modal dialog
│       │   ├── AnalyticsPanel.java     # Charts & analytics view
│       │   ├── BudgetPanel.java        # Budget management view
│       │   ├── SettingsPanel.java      # Settings view
│       │   └── components/             # Reusable UI components
│       ├── cli/
│       │   └── ConsoleApp.java         # CLI interactive mode
│       └── test/                       # Tests
├── bin/                                # Compiled .class files (auto-generated)
├── DailyExpenseTracker.jar             # Pre-built executable JAR
├── run_gui.bat                         # Windows: Build & launch GUI
├── run_cli.bat                         # Windows: Build & launch CLI
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Java JDK 11 or later** — [Download here](https://adoptium.net/)
- Verify installation: `java -version`

---

### ▶️ Option 1: Run the pre-built JAR

```bash
java -jar DailyExpenseTracker.jar
```

To launch in CLI mode:

```bash
java -jar DailyExpenseTracker.jar --cli
```

---

### 🔨 Option 2: Build from Source (Windows)

**GUI Mode:**
```bat
run_gui.bat
```

**CLI Mode:**
```bat
run_cli.bat
```

---

### 🔨 Option 3: Build Manually (Cross-platform)

**Compile:**
```bash
javac -d bin -sourcepath src \
  src/com/expensetracker/Main.java \
  src/com/expensetracker/model/*.java \
  src/com/expensetracker/storage/*.java \
  src/com/expensetracker/service/*.java \
  src/com/expensetracker/ui/*.java \
  src/com/expensetracker/ui/components/*.java \
  src/com/expensetracker/cli/*.java
```

**Run GUI:**
```bash
java -cp bin com.expensetracker.Main
```

**Run CLI:**
```bash
java -cp bin com.expensetracker.Main --cli
```

---

## 🏷️ Expense Categories

| Category              | Description                   |
|-----------------------|-------------------------------|
| 🍔 Food & Dining       | Restaurants, groceries        |
| 🚌 Transportation      | Fuel, public transit, cabs    |
| ⚡ Utilities & Bills   | Electricity, water, internet  |
| 🎬 Entertainment       | Movies, games, subscriptions  |
| 🏥 Healthcare & Fitness| Doctor, gym, medicine         |
| 🛍️ Shopping & Retail   | Clothes, electronics          |
| 📚 Education & Learning| Books, courses, tuition       |
| 🏠 Housing & Rent      | Rent, maintenance             |
| 📦 Miscellaneous       | Everything else               |

---

## 🛠️ Tech Stack

| Layer       | Technology                |
|-------------|---------------------------|
| Language    | Java 11+                  |
| GUI         | Java Swing                |
| Storage     | Local JSON files          |
| Build       | Manual `javac` / BAT scripts |

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

## 🙋 Author

Built as a personal learning project in Java.
