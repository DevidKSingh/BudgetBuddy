package com.expensetracker;

import com.expensetracker.cli.ConsoleApp;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.ui.MainFrame;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * Main application entry point for the Daily Expense Tracker System.
 */
public class Main {

    public static void main(String[] args) {
        boolean forceCli = false;

        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                forceCli = true;
                break;
            }
        }

        ExpenseService service = new ExpenseService();

        if (forceCli || GraphicsEnvironment.isHeadless()) {
            ConsoleApp cli = new ConsoleApp(service);
            cli.run();
        } else {
            SwingUtilities.invokeLater(() -> {
                MainFrame frame = new MainFrame(service);
                frame.setVisible(true);
            });
        }
    }
}
