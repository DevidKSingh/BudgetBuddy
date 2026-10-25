package com.expensetracker.ui;

import com.expensetracker.model.Currency;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.ui.components.StyledButton;
import com.expensetracker.ui.components.StyledButton.ButtonType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Settings & Preferences panel allowing users to select active Currency and view app configuration with styled buttons.
 */
public class SettingsPanel extends JPanel {

    private final ExpenseService expenseService;
    private final Runnable onSettingsChanged;

    private JComboBox<String> currencyComboBox;
    private JLabel currentCurrencyDisplay;

    public SettingsPanel(ExpenseService expenseService, Runnable onSettingsChanged) {
        this.expenseService = expenseService;
        this.onSettingsChanged = onSettingsChanged;

        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(20, 25, 20, 25));
        setBackground(new Color(245, 247, 250));

        initUI();
    }

    private void initUI() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Header Title
        JLabel titleLabel = new JLabel("Application Settings & Preferences");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(20));

        // --- Card 1: Currency Selector ---
        JPanel currencyCard = new JPanel(new BorderLayout());
        currencyCard.setBackground(Color.WHITE);
        currencyCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(20, 25, 20, 25)
        ));
        currencyCard.setMaximumSize(new Dimension(800, 180));
        currencyCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel currCardTitle = new JLabel("Preferred Currency Unit");
        currCardTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        currCardTitle.setForeground(new Color(30, 41, 59));

        JPanel formGrid = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        formGrid.setOpaque(false);

        JLabel selectLbl = new JLabel("Select System Currency:");
        selectLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        selectLbl.setForeground(new Color(71, 85, 105));
        formGrid.add(selectLbl);

        currencyComboBox = new JComboBox<>(Currency.getAllDisplayNames());
        currencyComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        currencyComboBox.setPreferredSize(new Dimension(240, 38));
        currencyComboBox.setSelectedItem(expenseService.getCurrency().getDisplayName());
        formGrid.add(currencyComboBox);

        StyledButton saveCurrencyBtn = new StyledButton("Apply & Save Currency", ButtonType.PRIMARY, "Save chosen currency as system default across dashboard and reports");
        saveCurrencyBtn.addActionListener(e -> onSaveCurrency());
        formGrid.add(saveCurrencyBtn);

        currentCurrencyDisplay = new JLabel("Active Currency: " + expenseService.getCurrency().getDisplayName() + " (" + expenseService.getCurrency().getSymbol() + ")");
        currentCurrencyDisplay.setFont(new Font("Segoe UI", Font.BOLD, 14));
        currentCurrencyDisplay.setForeground(new Color(16, 185, 129));

        JPanel cardBody = new JPanel(new BorderLayout());
        cardBody.setOpaque(false);
        cardBody.add(formGrid, BorderLayout.NORTH);
        cardBody.add(currentCurrencyDisplay, BorderLayout.SOUTH);

        currencyCard.add(currCardTitle, BorderLayout.NORTH);
        currencyCard.add(cardBody, BorderLayout.CENTER);

        centerPanel.add(currencyCard);
        centerPanel.add(Box.createVerticalStrut(20));

        // --- Card 2: System Info ---
        JPanel infoCard = new JPanel(new BorderLayout());
        infoCard.setBackground(Color.WHITE);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(20, 25, 20, 25)
        ));
        infoCard.setMaximumSize(new Dimension(800, 160));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel infoTitle = new JLabel("Data Storage & System Information");
        infoTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        infoTitle.setForeground(new Color(30, 41, 59));

        JTextArea infoText = new JTextArea(
                "• All transactions and budget configurations are saved automatically to your local home directory.\n" +
                "• Storage Format: Robust Encoded JSON & Portable CSV\n" +
                "• Built for high accuracy, offline usage, and zero third-party cloud dependence."
        );
        infoText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoText.setEditable(false);
        infoText.setBackground(Color.WHITE);
        infoText.setForeground(new Color(71, 85, 105));

        infoCard.add(infoTitle, BorderLayout.NORTH);
        infoCard.add(infoText, BorderLayout.CENTER);

        centerPanel.add(infoCard);

        add(centerPanel, BorderLayout.NORTH);
    }

    private void onSaveCurrency() {
        String selectedName = (String) currencyComboBox.getSelectedItem();
        Currency newCurrency = Currency.fromName(selectedName);
        expenseService.setCurrency(newCurrency);

        currentCurrencyDisplay.setText("Active Currency: " + newCurrency.getDisplayName() + " (" + newCurrency.getSymbol() + ")");
        JOptionPane.showMessageDialog(this, "Currency updated to " + newCurrency.getDisplayName() + " (" + newCurrency.getSymbol() + ").", "Currency Updated", JOptionPane.INFORMATION_MESSAGE);

        if (onSettingsChanged != null) {
            onSettingsChanged.run();
        }
    }

    public void refreshSettingsView() {
        currencyComboBox.setSelectedItem(expenseService.getCurrency().getDisplayName());
        currentCurrencyDisplay.setText("Active Currency: " + expenseService.getCurrency().getDisplayName() + " (" + expenseService.getCurrency().getSymbol() + ")");
    }
}
