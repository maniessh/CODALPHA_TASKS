import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

/**
 * Task 3: AI Chatbot
 * Features: NLP intent classification, pattern matching, context memory,
 *           sentiment detection, FAQ knowledge base, typing animation,
 *           conversation history, modern dark GUI
 */
public class AIChatbot extends JFrame {

    // ─── NLP Engine ──────────────────────────────────────────────
    static class Intent {
        String name;
        List<String> patterns;
        List<String> responses;

        Intent(String name, List<String> patterns, List<String> responses) {
            this.name      = name;
            this.patterns  = patterns;
            this.responses = responses;
        }
    }

    static class NLPEngine {
        private final List<Intent> intents = new ArrayList<>();
        private final Random random = new Random();
        private final Map<String, String> context = new LinkedHashMap<>();

        NLPEngine() { loadIntents(); }

        private void loadIntents() {
            // Greeting
            addIntent("greeting",
                List.of("hello", "hi", "hey", "good morning", "good evening", "howdy", "sup"),
                List.of("Hello! 👋 How can I help you today?",
                        "Hey there! What can I do for you?",
                        "Hi! Great to see you. Ask me anything!"));

            // Farewell
            addIntent("farewell",
                List.of("bye", "goodbye", "see you", "exit", "quit", "later", "take care"),
                List.of("Goodbye! Have a great day! 👋",
                        "See you later! Take care! 😊",
                        "Bye! Come back anytime!"));

            // Thanks
            addIntent("thanks",
                List.of("thank", "thanks", "thank you", "appreciate", "helpful", "great help"),
                List.of("You're welcome! 😊",
                        "Happy to help! Anything else?",
                        "Anytime! That's what I'm here for."));

            // Name
            addIntent("name",
                List.of("your name", "who are you", "what are you", "introduce yourself", "what should i call you"),
                List.of("I'm JavaBot 🤖 — your AI assistant built in Java!",
                        "Call me JavaBot! I'm here to assist you.",
                        "I'm JavaBot, a Java-based chatbot with NLP capabilities!"));

            // Capabilities
            addIntent("capabilities",
                List.of("what can you do", "help", "capabilities", "features", "what do you know", "abilities"),
                List.of("I can answer FAQs, chat naturally, detect your mood, and help with common questions! Try asking about Java, time, jokes, or just chat. 🚀"));

            // Java
            addIntent("java",
                List.of("java", "programming", "code", "oop", "swing", "jvm", "class", "object", "inheritance", "polymorphism"),
                List.of("Java is a powerful OOP language! Key concepts: Classes, Objects, Inheritance, Polymorphism, Encapsulation, Abstraction. 💻",
                        "Java runs on the JVM, making it platform-independent. 'Write once, run anywhere!'",
                        "Java's Swing library (like this chatbot!) makes it easy to build desktop GUIs.",
                        "Java tip: Use ArrayList over arrays when you need dynamic sizing!"));

            // Time / Date
            addIntent("time",
                List.of("time", "date", "today", "current time", "what day", "what time"),
                List.of("__TIME__"));  // special token

            // Jokes
            addIntent("joke",
                List.of("joke", "funny", "laugh", "humor", "tell me something funny", "make me laugh"),
                List.of("Why do Java developers wear glasses? Because they don't C# 😄",
                        "I told a joke about Java... it had too many class issues! 😂",
                        "Why did the programmer quit? Because they didn't get arrays! 🤣",
                        "A SQL query walks into a bar, joins two tables and orders a UNION. 😆"));

            // Mood / How are you
            addIntent("mood",
                List.of("how are you", "are you ok", "how do you feel", "you alright", "you good"),
                List.of("I'm just a bot, but I'm running great! 100% uptime today 😄",
                        "All systems operational! How about you?",
                        "Feeling fantastic — just had a fresh JVM startup! ⚡"));

            // Math
            addIntent("math",
                List.of("calculate", "math", "add", "multiply", "what is \\d+", "\\d+ \\+ \\d+", "\\d+ \\* \\d+"),
                List.of("__MATH__"));

            // Negative sentiment
            addIntent("negative",
                List.of("bad", "terrible", "hate", "awful", "worst", "broken", "useless", "stupid", "doesn't work", "not working"),
                List.of("I'm sorry to hear that 😟 Could you describe the issue in more detail?",
                        "That sounds frustrating. I'll do my best to help!",
                        "Let me try to fix that for you. What went wrong?"));

            // Positive sentiment
            addIntent("positive",
                List.of("great", "awesome", "love", "amazing", "fantastic", "excellent", "perfect", "wonderful"),
                List.of("That's wonderful to hear! 😊",
                        "Glad to hear that! 🎉",
                        "Awesome! Keep up the great work! 🚀"));

            // Weather (simulated)
            addIntent("weather",
                List.of("weather", "rain", "sunny", "temperature", "forecast", "hot", "cold"),
                List.of("I don't have real-time weather data, but you can check weather.com or Google! ☀️🌧",
                        "For live weather, try asking Google Assistant or check your local forecast!"));

            // Stock / Finance
            addIntent("stock",
                List.of("stock", "share", "market", "nse", "bse", "nifty", "sensex", "invest", "trading"),
                List.of("I love trading! 📈 For live NSE/BSE data, use Zerodha Kite or NSE India website.",
                        "Nifty 50 is India's benchmark index on NSE. Always DYOR before investing! 📊",
                        "Key trading tip: Never risk more than 1-2% of capital on a single trade!"));

            // Default / fallback
            addIntent("default",
                List.of(),
                List.of("Hmm, I'm not sure about that. Could you rephrase?",
                        "Interesting! I'm still learning. Can you ask that differently?",
                        "I don't have an answer for that yet, but I'm always improving! 🤖",
                        "That's a bit beyond my knowledge base. Try asking something else?"));
        }

        private void addIntent(String name, List<String> patterns, List<String> responses) {
            intents.add(new Intent(name, patterns, responses));
        }

        // ── Core NLP: classify intent + generate response ──────
        String respond(String userInput) {
            String input = userInput.toLowerCase().trim();

            // Math expression shortcut: e.g. "12 + 5" or "3 * 4"
            Pattern mathPattern = Pattern.compile("(\\d+)\\s*([+\\-*/])\\s*(\\d+)");
            Matcher m = mathPattern.matcher(input);
            if (m.find()) {
                double a = Double.parseDouble(m.group(1));
                double b = Double.parseDouble(m.group(3));
                double result = switch (m.group(2)) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    case "/" -> b != 0 ? a / b : Double.NaN;
                    default  -> 0;
                };
                return Double.isNaN(result) ? "Can't divide by zero! 😅"
                    : String.format("= %.2f 🧮", result);
            }

            // Context memory: remember user's name
            Pattern nameCapture = Pattern.compile("my name is ([a-zA-Z]+)");
            Matcher nm = nameCapture.matcher(input);
            if (nm.find()) {
                context.put("name", nm.group(1));
                return "Nice to meet you, " + nm.group(1) + "! 😊 How can I help?";
            }
            if (input.contains("my name") && context.containsKey("name")) {
                return "Your name is " + context.get("name") + "!";
            }

            // Score each intent
            Intent bestIntent = null;
            int bestScore = 0;
            for (Intent intent : intents) {
                if (intent.name.equals("default")) continue;
                int score = scoreIntent(intent, input);
                if (score > bestScore) {
                    bestScore = score;
                    bestIntent = intent;
                }
            }

            // Pick response
            Intent chosen = (bestScore > 0 && bestIntent != null) ? bestIntent
                : intents.get(intents.size() - 1);  // fallback

            String resp = chosen.responses.get(random.nextInt(chosen.responses.size()));

            // Handle special tokens
            if (resp.equals("__TIME__")) {
                return "🕐 Current time: " + new java.util.Date().toString();
            }
            if (resp.equals("__MATH__")) {
                return "I can do math! Try: 12 + 5, 100 - 37, 6 * 9, 144 / 12 🧮";
            }

            // Personalise with name if known
            if (context.containsKey("name") && random.nextInt(4) == 0) {
                resp += " " + context.get("name") + "!";
            }
            return resp;
        }

        private int scoreIntent(Intent intent, String input) {
            int score = 0;
            for (String pattern : intent.patterns) {
                if (input.contains(pattern)) {
                    score += pattern.split(" ").length; // longer match = higher score
                }
            }
            return score;
        }
    }

    // ─── GUI State ───────────────────────────────────────────────
    private JTextPane chatPane;
    private JTextField inputField;
    private final NLPEngine nlp = new NLPEngine();
    private final StyledDocument doc;
    private static final Color BOT_BG   = new Color(40, 55, 80);
    private static final Color USER_BG  = new Color(0, 110, 60);
    private static final Color DARK_BG  = new Color(18, 22, 34);

    // ─── Constructor ─────────────────────────────────────────────
    public AIChatbot() {
        setTitle("🤖 JavaBot — AI Chatbot");
        setSize(700, 620);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(DARK_BG);

        // Chat pane
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(DARK_BG);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        add(scroll, BorderLayout.CENTER);

        add(buildInputBar(), BorderLayout.SOUTH);
        add(buildTopBar(),   BorderLayout.NORTH);

        doc = chatPane.getStyledDocument();
        appendBotMessage("👋 Hello! I'm JavaBot, your AI assistant.\nAsk me anything — Java, jokes, time, math, stocks, or just chat!");

        setVisible(true);
    }

    // ─── Top Bar ─────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(12, 16, 26));
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel name = new JLabel("🤖 JavaBot  •  NLP-Powered  •  Always Learning");
        name.setFont(new Font("Segoe UI", Font.BOLD, 15));
        name.setForeground(new Color(100, 180, 255));
        p.add(name, BorderLayout.WEST);

        JLabel status = new JLabel("● Online");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        status.setForeground(new Color(0, 220, 100));
        p.add(status, BorderLayout.EAST);
        return p;
    }

    // ─── Input Bar ───────────────────────────────────────────────
    private JPanel buildInputBar() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(new Color(22, 28, 42));
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(new Color(35, 42, 60));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 80, 120), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        inputField.addActionListener(e -> sendMessage());

        JButton sendBtn = new JButton("Send ➤");
        sendBtn.setBackground(new Color(0, 120, 220));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        JButton clearBtn = new JButton("🗑");
        clearBtn.setToolTipText("Clear chat");
        clearBtn.setBackground(new Color(60, 60, 75));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        clearBtn.setBorderPainted(false);
        clearBtn.addActionListener(e -> {
            try { doc.remove(0, doc.getLength()); } catch (Exception ignored) {}
            appendBotMessage("Chat cleared! How can I help?");
        });

        p.add(clearBtn,   BorderLayout.WEST);
        p.add(inputField, BorderLayout.CENTER);
        p.add(sendBtn,    BorderLayout.EAST);
        return p;
    }

    // ─── Messaging ───────────────────────────────────────────────
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");
        appendUserMessage(text);

        // Typing animation then reply
        Timer timer = new Timer(600, e -> {
            String reply = nlp.respond(text);
            appendBotMessage(reply);
        });
        timer.setRepeats(false);
        timer.start();
        appendTypingIndicator();
    }

    private void appendUserMessage(String text) {
        appendBubble("  You  ", text, new Color(200, 240, 210), USER_BG, true);
    }

    private void appendBotMessage(String text) {
        appendBubble("  JavaBot  ", text, new Color(180, 210, 255), BOT_BG, false);
    }

    private void appendTypingIndicator() {
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, new Color(100, 120, 160));
        StyleConstants.setFontFamily(set, "Segoe UI");
        StyleConstants.setFontSize(set, 13);
        try {
            doc.insertString(doc.getLength(), "\nJavaBot is typing...\n", set);
            scrollToBottom();
        } catch (Exception ignored) {}

        // Remove typing indicator after delay
        Timer t = new Timer(580, e -> {
            try {
                String content = doc.getText(0, doc.getLength());
                int idx = content.lastIndexOf("\nJavaBot is typing...\n");
                if (idx >= 0) doc.remove(idx, "\nJavaBot is typing...\n".length());
            } catch (Exception ignored) {}
        });
        t.setRepeats(false); t.start();
    }

    private void appendBubble(String label, String text, Color labelColor, Color bg, boolean alignRight) {
        try {
            // Sender label
            SimpleAttributeSet labelStyle = new SimpleAttributeSet();
            StyleConstants.setFontFamily(labelStyle, "Segoe UI");
            StyleConstants.setFontSize(labelStyle, 11);
            StyleConstants.setForeground(labelStyle, labelColor);
            StyleConstants.setBold(labelStyle, true);
            doc.insertString(doc.getLength(), "\n" + (alignRight ? "                              " : "") + label + "\n", labelStyle);

            // Message
            SimpleAttributeSet msgStyle = new SimpleAttributeSet();
            StyleConstants.setFontFamily(msgStyle, "Segoe UI");
            StyleConstants.setFontSize(msgStyle, 14);
            StyleConstants.setForeground(msgStyle, new Color(230, 235, 245));
            StyleConstants.setBackground(msgStyle, bg);
            String prefix = alignRight ? "                    " : "";
            doc.insertString(doc.getLength(), prefix + text + "\n", msgStyle);
            scrollToBottom();
        } catch (Exception ignored) {}
    }

    private void scrollToBottom() {
        chatPane.setCaretPosition(doc.getLength());
    }

    // ─── Entry ───────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(AIChatbot::new);
    }
}
