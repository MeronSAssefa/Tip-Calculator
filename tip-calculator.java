import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;

public class TipCalculator extends JFrame {

    private final JTextField billField = new JTextField();
    private final JTextField customTipField = new JTextField();
    private final JSpinner peopleSpinner =
            new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JComboBox<String> roundingBox =
            new JComboBox<>(new String[]{"No rounding", "Round total", "Round per person"});

    // Preset tip buttons
    private final double[] presetTips = {0.10, 0.12, 0.15, 0.18, 0.20, 0.25};
    private Double selectedTip = null; // decimal (e.g., 0.15)

    // Results
    private final JLabel tipAmountLbl = new JLabel("$0.00");
    private final JLabel totalLbl = new JLabel("$0.00");
    private final JLabel perPersonLbl = new JLabel("$0.00");
    private final JLabel statusLbl = new JLabel("Ready");

    private final NumberFormat money = NumberFormat.getCurrencyInstance();

    public TipCalculator() {
        super("💸 Tip Calculator (Java)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(null);

        var root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        // Header
        var header = new JPanel(new BorderLayout());
        var title = new JLabel("💸 Tip Calculator", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        statusLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLbl.setForeground(new Color(148, 163, 184));
        header.add(title, BorderLayout.WEST);
        header.add(statusLbl, BorderLayout.EAST);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));
        root.add(header, BorderLayout.NORTH);

        // Split grid
        var grid = new JPanel(new GridLayout(1, 2, 12, 12));
        root.add(grid, BorderLayout.CENTER);

        // Left card (inputs)
        var inputs = card();
        inputs.setLayout(new GridBagLayout());
        GridBagConstraints c = gbc();

        // Bill input
        addLabel(inputs, "Bill", c);
        styleText(billField, "e.g. 42.50");
        inputs.add(billField, grow(c));

        // Preset tip buttons
        c.gridy++;
        inputs.add(space(6), grow(c));
        c.gridy++;
        inputs.add(new JLabel("Tip Presets"), grow(c));
        c.gridy++;
        var presets = new JPanel(new GridLayout(2, 3, 8, 8));
        for (double p : presetTips) {
            String text = (int) Math.round(p * 100) + "%";
            var btn = new JToggleButton(text);
            btn.addActionListener(e -> {
                clearPresetSelection(presets);
                btn.setSelected(true);
                selectedTip = p;
                status("Tip set to " + (int) Math.round(p * 100) + "%");
            });
            presets.add(btn);
        }
        inputs.add(presets, grow(c));

        // Custom tip
        c.gridy++;
        inputs.add(new JLabel("Custom Tip %"), grow(c));
        c.gridy++;
        styleText(customTipField, "e.g. 17");
        inputs.add(customTipField, grow(c));

        // People + Rounding
        c.gridy++;
        var row = new JPanel(new GridLayout(1, 2, 8, 8));
        var peoplePanel = new JPanel(new BorderLayout(6, 6));
        peoplePanel.add(new JLabel("People"), BorderLayout.NORTH);
        peoplePanel.add(peopleSpinner, BorderLayout.CENTER);
        var roundPanel = new JPanel(new BorderLayout(6, 6));
        roundPanel.add(new JLabel("Rounding"), BorderLayout.NORTH);
        roundPanel.add(roundingBox, BorderLayout.CENTER);
        row.add(peoplePanel);
        row.add(roundPanel);
        inputs.add(row, grow(c));

        // Actions
        c.gridy++;
        var actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        var calcBtn = new JButton("Calculate");
        var copyBtn = new JButton("Copy Summary");
        var resetBtn = new JButton("Reset");
        actions.add(calcBtn);
        actions.add(copyBtn);
        actions.add(resetBtn);
        inputs.add(actions, grow(c));

        // Right card (results)
        var results = card();
        results.setLayout(new GridLayout(6, 1, 8, 8));
        results.add(sectionTitle("Results"));
        results.add(line("Tip Amount", tipAmountLbl));
        results.add(line("Total", totalLbl));
        results.add(line("Per Person", perPersonLbl));

        grid.add(inputs);
        grid.add(results);

        // Actions
        calcBtn.addActionListener(this::compute);
        copyBtn.addActionListener(e -> copySummary());
        resetBtn.addActionListener(e -> reset(presets));

        // Enter key triggers calculate
        getRootPane().setDefaultButton(calcBtn);
    }

    private void compute(ActionEvent e) {
        try {
            double bill = Double.parseDouble(billField.getText().trim());
            if (bill <= 0) {
                status("Enter a valid bill amount");
                return;
            }

            // tip percentage: either preset or custom
            Double tipPct = selectedTip;
            String customTxt = customTipField.getText().trim();
            if (!customTxt.isEmpty()) {
                tipPct = Double.parseDouble(customTxt) / 100.0;
            }
            if (tipPct == null || tipPct < 0) {
                status("Pick a tip or enter a custom %");
                return;
            }

            int people = (Integer) peopleSpinner.getValue();
            double tipAmount = bill * tipPct;
            double total = bill + tipAmount;
            double perPerson = total / people;

            String rounding = (String) roundingBox.getSelectedItem();
            if ("Round total".equals(rounding)) {
                total = Math.round(total);      // to whole currency unit
                perPerson = total / people;
            } else if ("Round per person".equals(rounding)) {
                perPerson = Math.round(perPerson);
                total = perPerson * people;
            }

            tipAmountLbl.setText(money(tipAmount));
            totalLbl.setText(money(total));
            perPersonLbl.setText(money(perPerson));
            status("Calculated");
        } catch (NumberFormatException ex) {
            status("Invalid number format");
        }
    }

    private void copySummary() {
        double bill = parseDoubleSafe(billField.getText());
        int people = (Integer) peopleSpinner.getValue();
        String tipTxt = customTipField.getText().trim();
        String tipPctStr = (tipTxt.isEmpty() && selectedTip != null)
                ? String.format("%.1f", selectedTip * 100)
                : (tipTxt.isEmpty() ? "—" : tipTxt);

        String summary = "Bill: " + money(bill) + "\n" +
                "Tip: " + tipPctStr + "%\n" +
                "People: " + people + "\n" +
                "—\n" +
                "Tip Amount: " + tipAmountLbl.getText() + "\n" +
                "Total: " + totalLbl.getText() + "\n" +
                "Per Person: " + perPersonLbl.getText();

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(summary), null);

        status("Summary copied to clipboard");
    }

    private void reset(JPanel presets) {
        billField.setText("");
        customTipField.setText("");
        peopleSpinner.setValue(1);
        roundingBox.setSelectedIndex(0);
        selectedTip = null;
        clearPresetSelection(presets);
        tipAmountLbl.setText("$0.00");
        totalLbl.setText("$0.00");
        perPersonLbl.setText("$0.00");
        status("Ready");
    }

    // -------- UI helpers --------
    private static JPanel card() {
        var p = new JPanel();
        p.setBorder(new EmptyBorder(12, 12, 12, 12));
        p.setBackground(new Color(14, 22, 41));
        p.setOpaque(true);
        p.setLayout(new BorderLayout());
        return p;
    }

    private static GridBagConstraints gbc() {
        var c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0;
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0; c.weighty = 0;
        return c;
    }

    private static Component sectionTitle(String text) {
        var lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
        lbl.setForeground(new Color(203, 213, 225));
        return lbl;
    }

    private static JPanel line(String left, JLabel rightValue) {
        var row = new JPanel(new BorderLayout());
        row.setBorder(new EmptyBorder(10, 12, 10, 12));
        row.setBackground(new Color(10, 18, 34));
        var leftLbl = new JLabel(left);
        leftLbl.setForeground(new Color(148, 163, 184));
        rightValue.setFont(rightValue.getFont().deriveFont(Font.BOLD, 22f));
        rightValue.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(leftLbl, BorderLayout.WEST);
        row.add(rightValue, BorderLayout.EAST);
        return row;
    }

    private static void addLabel(JPanel parent, String text, GridBagConstraints c) {
        var lbl = new JLabel(text);
        lbl.setForeground(new Color(148, 163, 184));
        parent.add(lbl, c);
        c.gridy++;
    }

    private static GridBagConstraints grow(GridBagConstraints c) {
        var cc = (GridBagConstraints) c.clone();
        cc.weightx = 1.0;
        cc.fill = GridBagConstraints.HORIZONTAL;
        return cc;
    }

    private static Component space(int px) {
        var s = Box.createRigidArea(new Dimension(0, px));
        s.setBackground(new Color(14, 22, 41));
        return s;
    }

    private static void styleText(JTextField tf, String placeholder) {
        tf.putClientProperty("JTextField.placeholderText", placeholder); // works in some LAFs
        tf.setColumns(10);
    }

    private static void clearPresetSelection(JPanel presets) {
        for (Component comp : presets.getComponents()) {
            if (comp instanceof JToggleButton btn) {
                btn.setSelected(false);
            }
        }
    }

    private String money(double n) {
        if (Double.isFinite(n)) return money.format(n);
        return "$0.00";
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }

    private void status(String s) {
        statusLbl.setText(s);
    }

    // ---- main ----
    public static void main(String[] args) {
        // Simple dark look
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new TipCalculator().setVisible(true));
    }
}
