# 💰 Daily Expense & Income Tracker

> **Version: 2.1.0** | Built with Java 11+, Java Swing, and Clean MVC Architecture.

A comprehensive **Java desktop personal finance application** for managing daily income and expenditures. Features an ultra-responsive, modernized Swing GUI and an interactive terminal CLI mode. It includes interactive calendar drop-downs, batch multi-record deletion, real-time monthly budget alerts, visual charts (donut & grouped bar), CSV import/export, and local JSON persistence.

---

## ✨ Key Features

- 💚 **Dual Income & Expense Tracking** — Record expenses or income sources (Salary, Freelance, Scholarship, Gift, Investment, Rental, etc.) with dedicated color-coded workflows.
- 📅 **Modern Interactive Calendar Picker** — Drop-down calendar component (`DatePickerField`) with clean month/year navigation, weekend color accents, quick "Today" shortcut, and clear actions across all transaction forms and filters.
- 🗑️ **Multi-Row Batch Deletion** — Select multiple transactions via `Ctrl+Click` or `Shift+Click` and delete them simultaneously with a single confirmation prompt (available in both GUI and CLI).
- 🖱️ **Context Menu with Custom Vector Icons** — Right-click any transaction row for quick actions (Edit & Delete) featuring custom-drawn Java2D icons.
- 📊 **Executive Dashboard** — 4 responsive KPI cards: Total Income, Total Expenses, Net Balance, and Monthly Budget Status with dynamic progress visualization.
- 📋 **Unified Transaction Table** — Filter instantly by transaction type (All / Income / Expenses), date range, category, or keyword with live summary totals.
- 📈 **Visual Analytics & Charts** — Expense category donut chart with hover effects alongside a 6-month Income vs. Expenses grouped comparison bar chart.
- 💰 **Budget & Proactive Overrun Alerts** — Set monthly spending caps; visual banners and badges notify you upon approaching (80%) or exceeding (100%) limits.
- ⚙️ **Multi-Currency Support** — Seamlessly switch currencies (USD `$`, EUR `€`, GBP `£`, INR `₹`, JPY `¥`, CAD `C$`, and more) saved across all views.
- 💾 **Robust Offline Persistence** — Quote-aware JSON storage saved directly to `~/.expense_tracker_data/` with zero third-party dependencies.
- 📤 **CSV Export & Import** — Export full transaction history (including Type, Category, Amount, Date, and Notes) with backward-compatible CSV parser.
- 🖥️ **Dual Run Modes** — Launches into a polished desktop GUI by default, or an interactive terminal CLI when invoked with `--cli`.

---

## 🗂️ Project Structure

```
Daily Expense Tracker/
├── src/
│   └── com/expensetracker/
│       ├── Main.java                          # Main application entry point
│       ├── cli/
│       │   └── ConsoleApp.java                # Full interactive terminal CLI
│       ├── model/
│       │   ├── Expense.java                   # Transaction model (Income & Expense)
│       │   ├── ExpenseCategory.java           # Expense categories enum
│       │   ├── IncomeCategory.java            # Income source categories enum
│       │   ├── Budget.java                    # Monthly budget limits model
│       │   └── Currency.java                  # Currency definitions & formatting
│       ├── service/
│       │   └── ExpenseService.java            # Business logic, aggregations, & CRUD
│       ├── storage/
│       │   ├── StorageManager.java            # Quote-aware JSON local persistence
│       │   └── CsvExporter.java               # Type-preserving CSV export & import
│       ├── ui/
│       │   ├── MainFrame.java                 # Main window frame & sidebar navigation
│       │   ├── DashboardPanel.java            # KPI overview & recent transactions
│       │   ├── ExpenseTablePanel.java         # Transactions table & multi-filter bar
│       │   ├── ExpenseDialog.java             # Log/edit expense modal with date picker
│       │   ├── IncomeDialog.java              # Log/edit income modal with date picker
│       │   ├── AnalyticsPanel.java            # Category donut & comparison bar charts
│       │   ├── BudgetPanel.java               # Monthly budget configuration & tracking
│       │   ├── SettingsPanel.java             # Currency preferences & storage info
│       │   └── components/
│       │       ├── DatePickerField.java       # Custom modern drop-down calendar
│       │       ├── MenuIcons.java             # Java2D vector menu icons
│       │       └── StyledButton.java          # Custom styled buttons with hover effects
│       └── test/
│           └── ExpenseTrackerTest.java        # 12 automated unit & integration tests
├── bin/                                       # Compiled bytecode classes
├── DailyExpenseTracker.jar                    # Pre-packaged executable JAR
├── run_gui.bat                                # Windows 1-click build & launch GUI
├── run_cli.bat                                # Windows 1-click build & launch CLI
├── .gitignore                                 # Git ignore rules
├── LICENSE                                    # MIT License
└── README.md                                  # Documentation
```

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK) 11 or later** — [Download Eclipse Temurin / Adoptium](https://adoptium.net/)
- Verify your Java installation:
  ```bash
  java -version
  javac -version
  ```

---

### ▶️ Option 1: Run the Pre-Built JAR

**Launch GUI Mode:**
```bash
java -jar DailyExpenseTracker.jar
```

**Launch CLI Mode:**
```bash
java -jar DailyExpenseTracker.jar --cli
```

---

### 🔨 Option 2: 1-Click Launchers (Windows)

- Double-click **`run_gui.bat`** to compile the source code, update the JAR, and launch the graphical interface.
- Double-click **`run_cli.bat`** to compile and start the interactive terminal interface.

---

### 🔨 Option 3: Compile & Run Manually (Cross-Platform)

**1. Compile All Sources:**
```bash
javac -d bin -sourcepath src \
  src/com/expensetracker/Main.java \
  src/com/expensetracker/model/*.java \
  src/com/expensetracker/storage/*.java \
  src/com/expensetracker/service/*.java \
  src/com/expensetracker/ui/*.java \
  src/com/expensetracker/ui/components/*.java \
  src/com/expensetracker/cli/*.java \
  src/com/expensetracker/test/*.java
```

**2. Package Executable JAR:**
```bash
jar cfe DailyExpenseTracker.jar com.expensetracker.Main -C bin com
```

**3. Run GUI Mode:**
```bash
java -cp bin com.expensetracker.Main
```

**4. Run CLI Mode:**
```bash
java -cp bin com.expensetracker.Main --cli
```

**5. Run Automated Tests:**
```bash
java -cp bin com.expensetracker.test.ExpenseTrackerTest
```

---

## 🏷️ Category Reference

### Expense Categories
| Category | Description |
| :--- | :--- |
| 🍔 **Food & Dining** | Restaurants, groceries, cafes, deliveries |
| 🚌 **Transportation** | Fuel, public transit, rideshare, maintenance |
| ⚡ **Utilities & Bills** | Electricity, water, internet, phone bills |
| 🎬 **Entertainment** | Movies, gaming, subscriptions, concerts |
| 🏥 **Healthcare & Fitness** | Doctors, pharmacy, gym memberships |
| 🛍️ **Shopping & Retail** | Apparel, gadgets, home goods |
| 📚 **Education & Learning** | Books, tuition, online courses |
| 🏠 **Housing & Rent** | Rent, mortgage, property tax, maintenance |
| 📦 **Miscellaneous** | General expenses and miscellaneous items |

### Income Categories
| Category | Description |
| :--- | :--- |
| 💼 **Salary** | Regular monthly salary and wages |
| 💻 **Freelance** | Freelance client projects and consulting |
| 🎓 **Scholarship** | Academic scholarships, grants, and stipends |
| 🎁 **Gift** | Monetary gifts, bonuses, and rewards |
| 📈 **Investment** | Stock dividends, capital gains, interest |
| 🏠 **Rental** | Rental property income and leasing |
| 💵 **Other** | Other miscellaneous earnings |

---

## 🧪 Automated Testing

The project includes an end-to-end automated functional test suite (`ExpenseTrackerTest`) verifying:
1. Expense logging and validation
2. Real-time balance and aggregation calculations
3. Multi-criteria filtering (category, date range, keywords)
4. Update and single/multi-deletion operations
5. Budget threshold alerts (`OK`, `WARNING`, `EXCEEDED`)
6. Data persistence across application reloads
7. Type-preserving CSV import and export with duplicate prevention
8. Active currency switching and display formatting
9. Income logging and net balance calculations
10. Transaction type conversions (Expense ↔ Income)
11. Special character and comma handling in notes
12. CSV export preserving `isIncome` tags with backward compatibility

Run tests anytime with:
```bash
java -cp bin com.expensetracker.test.ExpenseTrackerTest
```

---

## 🛠️ Tech Stack & Architecture

- **Language:** Java 11+
- **GUI Framework:** Java Swing (`JFrame`, `CardLayout`, `JTable`, `JWindow`, custom `Graphics2D`)
- **Data Persistence:** Local quote-safe JSON and RFC-4180-compliant CSV
- **Architecture Pattern:** Model-View-Controller (MVC) separating UI components, business logic services, and persistence layers

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
