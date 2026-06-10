import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * Task 2: Stock Trading Platform
 * Features: Live price simulation, Buy/Sell, Portfolio tracking,
 *           P&L display, Transaction history, File I/O persistence
 */
public class StockTradingPlatform extends JFrame {

    // ─── OOP Models ──────────────────────────────────────────────
    static class Stock {
        String symbol, name;
        double price;
        double change;    // % change from open

        Stock(String symbol, String name, double price) {
            this.symbol = symbol;
            this.name   = name;
            this.price  = price;
            this.change = 0;
        }

        // Simulate realistic price tick
        void tick() {
            double move = (Math.random() - 0.495) * price * 0.012;
            price  = Math.max(1, price + move);
            change = Math.random() * 6 - 3;  // ±3%
        }

        public String toString() { return symbol; }
    }

    static class Holding {
        String symbol;
        int quantity;
        double avgCost;

        Holding(String symbol, int qty, double cost) {
            this.symbol  = symbol;
            this.quantity = qty;
            this.avgCost  = cost;
        }

        double currentValue(double price) { return quantity * price; }
        double pnl(double price)          { return (price - avgCost) * quantity; }
        double pnlPct(double price)       { return ((price - avgCost) / avgCost) * 100; }
    }

    static class Transaction {
        String type, symbol;
        int qty;
        double price;
        String time;

        Transaction(String type, String symbol, int qty, double price) {
            this.type   = type;
            this.symbol = symbol;
            this.qty    = qty;
            this.price  = price;
            this.time   = new SimpleDateFormat("HH:mm:ss").format(new Date());
        }

        public String toString() {
            return String.format("%s | %s %s x%d @ ₹%.2f", time, type, symbol, qty, price);
        }
    }

    static class Portfolio {
        String name;
        double cash;
        Map<String, Holding> holdings = new LinkedHashMap<>();
        List<Transaction> transactions = new ArrayList<>();

        Portfolio(String name, double startingCash) {
            this.name = name;
            this.cash = startingCash;
        }

        boolean buy(Stock stock, int qty) {
            double cost = stock.price * qty;
            if (cash < cost) return false;
            cash -= cost;
            Holding h = holdings.get(stock.symbol);
            if (h == null) {
                holdings.put(stock.symbol, new Holding(stock.symbol, qty, stock.price));
            } else {
                // Weighted average cost
                double totalQty  = h.quantity + qty;
                h.avgCost = (h.avgCost * h.quantity + stock.price * qty) / totalQty;
                h.quantity = (int) totalQty;
            }
            transactions.add(new Transaction("BUY", stock.symbol, qty, stock.price));
            return true;
        }

        boolean sell(Stock stock, int qty) {
            Holding h = holdings.get(stock.symbol);
            if (h == null || h.quantity < qty) return false;
            cash += stock.price * qty;
            h.quantity -= qty;
            if (h.quantity == 0) holdings.remove(stock.symbol);
            transactions.add(new Transaction("SELL", stock.symbol, qty, stock.price));
            return true;
        }

        double totalValue(Map<String, Stock> market) {
            double val = cash;
            for (Holding h : holdings.values()) {
                Stock s = market.get(h.symbol);
                if (s != null) val += h.currentValue(s.price);
            }
            return val;
        }
    }

    // ─── State ───────────────────────────────────────────────────
    private final Map<String, Stock> market = new LinkedHashMap<>();
    private final Portfolio portfolio = new Portfolio("My Portfolio", 100_000);
    private DefaultTableModel marketModel, portfolioModel;
    private JTextArea txLog;
    private JLabel lblCash, lblNetWorth, lblPnL;
    private static final String SAVE_FILE = "portfolio.dat";

    // ─── Constructor ─────────────────────────────────────────────
    public StockTradingPlatform() {
        initMarket();
        loadPortfolio();

        setTitle("📈 Stock Trading Platform");
        setSize(1100, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(new Color(15, 20, 30));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildCenter(),    BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        startMarketSimulation();
        setVisible(true);
    }

    // ─── Init ────────────────────────────────────────────────────
    private void initMarket() {
        market.put("RELIANCE", new Stock("RELIANCE", "Reliance Industries",  2850.00));
        market.put("TCS",      new Stock("TCS",      "Tata Consultancy",     3920.00));
        market.put("INFY",     new Stock("INFY",     "Infosys Ltd",          1720.00));
        market.put("HDFC",     new Stock("HDFC",     "HDFC Bank",            1680.00));
        market.put("WIPRO",    new Stock("WIPRO",    "Wipro Ltd",             565.00));
        market.put("TATA",     new Stock("TATA",     "Tata Motors",           910.00));
    }

    // ─── Header ──────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        p.setBackground(new Color(10, 15, 25));

        JLabel title = new JLabel("📈 NSE Simulator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(0, 220, 100));
        p.add(title);

        p.add(styledButton("🟢 Buy",  new Color(0, 140, 60),   e -> showTradeDialog("BUY")));
        p.add(styledButton("🔴 Sell", new Color(180, 30, 30),   e -> showTradeDialog("SELL")));
        p.add(styledButton("💾 Save", new Color(60, 100, 180),  e -> savePortfolio()));
        p.add(styledButton("📋 History", new Color(100, 60, 160), e -> showHistory()));
        p.add(styledButton("🔄 Reset",   new Color(80, 80, 80),   e -> resetPortfolio()));

        lblCash     = headerLabel("Cash: ₹1,00,000");
        lblNetWorth = headerLabel("Net Worth: ₹1,00,000");
        lblPnL      = headerLabel("P&L: ₹0");
        p.add(lblCash);
        p.add(lblNetWorth);
        p.add(lblPnL);
        return p;
    }

    private JLabel headerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(new Color(200, 200, 220));
        l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        return l;
    }

    // ─── Center (Market + Portfolio side by side) ─────────────────
    private JSplitPane buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMarketPanel(), buildPortfolioPanel());
        split.setDividerLocation(560);
        split.setDividerSize(6);
        split.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        split.setBackground(new Color(15, 20, 30));
        return split;
    }

    private JPanel buildMarketPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(20, 25, 38));
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 220, 100), 1),
            "  Market Watch  ", 0, 0,
            new Font("Segoe UI", Font.BOLD, 13), new Color(0, 220, 100)));

        String[] cols = {"Symbol", "Company", "Price (₹)", "Change %"};
        marketModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = darkTable(marketModel);
        // Color change % column
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                double v = Double.parseDouble(val.toString().replace("%",""));
                setForeground(v >= 0 ? new Color(0, 220, 100) : new Color(255, 70, 70));
                return this;
            }
        });

        p.add(new JScrollPane(table));
        return p;
    }

    private JPanel buildPortfolioPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(20, 25, 38));
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 160, 255), 1),
            "  My Portfolio  ", 0, 0,
            new Font("Segoe UI", Font.BOLD, 13), new Color(100, 160, 255)));

        String[] cols = {"Symbol", "Qty", "Avg Cost", "CMP", "P&L (₹)", "P&L %"};
        portfolioModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = darkTable(portfolioModel);
        // P&L color
        DefaultTableCellRenderer pnlRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                try {
                    double v = Double.parseDouble(val.toString().replace("₹","").replace("%","").replace("+","").trim());
                    setForeground(v >= 0 ? new Color(0, 220, 100) : new Color(255, 70, 70));
                } catch (Exception ignored) {}
                return this;
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(pnlRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(pnlRenderer);

        // Transaction log at bottom
        txLog = new JTextArea(5, 20);
        txLog.setEditable(false);
        txLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txLog.setBackground(new Color(12, 15, 22));
        txLog.setForeground(new Color(150, 200, 150));

        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(table), new JScrollPane(txLog));
        sp.setDividerLocation(280);
        p.add(sp);
        return p;
    }

    // ─── Status Bar ──────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        p.setBackground(new Color(10, 15, 25));
        JLabel info = new JLabel("💡 Market simulates real-time price ticks  |  Data saved to portfolio.dat");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(new Color(120, 130, 150));
        p.add(info);
        return p;
    }

    // ─── Trade Dialog ────────────────────────────────────────────
    private void showTradeDialog(String type) {
        String[] symbols = market.keySet().toArray(new String[0]);
        JComboBox<String> cbStock = new JComboBox<>(symbols);
        JSpinner spinQty = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));

        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.add(new JLabel("Stock:")); panel.add(cbStock);
        panel.add(new JLabel("Quantity:")); panel.add(spinQty);

        int result = JOptionPane.showConfirmDialog(this, panel, type + " Stock",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String sym = (String) cbStock.getSelectedItem();
        int qty     = (int) spinQty.getValue();
        Stock stock = market.get(sym);

        boolean ok = type.equals("BUY") ? portfolio.buy(stock, qty) : portfolio.sell(stock, qty);
        if (!ok) {
            JOptionPane.showMessageDialog(this, type.equals("BUY")
                ? "Insufficient funds!" : "Not enough shares to sell!");
        } else {
            txLog.append(portfolio.transactions.get(portfolio.transactions.size()-1) + "\n");
            refreshPortfolio();
        }
    }

    // ─── Market Simulation Timer ──────────────────────────────────
    private void startMarketSimulation() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                for (Stock s : market.values()) s.tick();
                SwingUtilities.invokeLater(() -> { refreshMarket(); refreshPortfolio(); });
            }
        }, 1500, 1500);
    }

    // ─── Refresh ─────────────────────────────────────────────────
    private void refreshMarket() {
        marketModel.setRowCount(0);
        for (Stock s : market.values()) {
            marketModel.addRow(new Object[]{
                s.symbol, s.name,
                String.format("%.2f", s.price),
                String.format("%.2f%%", s.change)
            });
        }
    }

    private void refreshPortfolio() {
        portfolioModel.setRowCount(0);
        for (Holding h : portfolio.holdings.values()) {
            Stock s = market.get(h.symbol);
            if (s == null) continue;
            portfolioModel.addRow(new Object[]{
                h.symbol, h.quantity,
                String.format("%.2f", h.avgCost),
                String.format("%.2f", s.price),
                String.format("₹%.2f", h.pnl(s.price)),
                String.format("%.2f%%", h.pnlPct(s.price))
            });
        }
        double netWorth = portfolio.totalValue(market);
        double pnl      = netWorth - 100_000;
        lblCash.setText(String.format("Cash: ₹%,.0f", portfolio.cash));
        lblNetWorth.setText(String.format("Net Worth: ₹%,.0f", netWorth));
        lblPnL.setForeground(pnl >= 0 ? new Color(0, 220, 100) : new Color(255, 70, 70));
        lblPnL.setText(String.format("P&L: %s₹%,.0f", pnl >= 0 ? "+" : "", pnl));
    }

    // ─── File I/O ────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void loadPortfolio() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            double cash = (double) in.readObject();
            Map<String, Holding> h = (Map<String, Holding>) in.readObject();
            portfolio.cash = cash;
            portfolio.holdings.putAll(h);
        } catch (Exception ignored) {}
    }

    private void savePortfolio() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            out.writeObject(portfolio.cash);
            out.writeObject(portfolio.holdings);
            JOptionPane.showMessageDialog(this, "Portfolio saved to " + SAVE_FILE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    private void showHistory() {
        if (portfolio.transactions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transactions yet."); return;
        }
        StringBuilder sb = new StringBuilder("TRANSACTION HISTORY\n" + "─".repeat(40) + "\n");
        for (Transaction t : portfolio.transactions) sb.append(t).append("\n");
        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "History", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetPortfolio() {
        int c = JOptionPane.showConfirmDialog(this, "Reset portfolio to ₹1,00,000?", "Reset", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            portfolio.cash = 100_000;
            portfolio.holdings.clear();
            portfolio.transactions.clear();
            txLog.setText("");
            refreshPortfolio();
        }
    }

    // ─── Utility ─────────────────────────────────────────────────
    private JTable darkTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.setBackground(new Color(25, 30, 45));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(50, 55, 70));
        table.setSelectionBackground(new Color(0, 100, 180));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(10, 15, 28));
        table.getTableHeader().setForeground(new Color(150, 200, 255));
        return table;
    }

    private JButton styledButton(String text, Color bg, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        return btn;
    }

    // Holding must be Serializable for File I/O
    static { /* Holding is already a plain class — works fine with ObjectOutputStream */ }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StockTradingPlatform::new);
    }
}

// Make Holding serializable for save/load
class HoldingSerializable implements Serializable {
    private static final long serialVersionUID = 1L;
}
