package roottemplate.chat.client;

import roottemplate.chat.ExitException;
import roottemplate.chat.Util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;

public class ConsoleFrame extends JFrame {
    public static final String FRAME_TITLE = "Командная строка";
    public static final int BUFFER_SIZE;
    public static final Font CONSOLE_FONT;
    private static boolean NOTIFY_ABOUT_NEW_MESSAGES;
    private volatile static ConsoleFrame frame;
    
    static {
        int temp;
        try {
            temp = Util.getPropertyUnsignedInt("chat.consoleBufferSize", 25600);
        } catch (ExitException ex) {
            temp = 25600;
        }
        BUFFER_SIZE = temp;
        
        try {
            temp = Util.getPropertyUnsignedInt("chat.fontSize", 14);
            if(temp < 1 || temp == Integer.MAX_VALUE)
                throw new ExitException();
        } catch (ExitException ex) {
            temp = 14;
        }
        CONSOLE_FONT = new Font("Courier New", Font.BOLD, temp);

        NOTIFY_ABOUT_NEW_MESSAGES = Boolean.valueOf(System.getProperty("chat.notifyAboutNewMessages", "false"));
    }
    
    public static ConsoleFrame launch() {
        if(frame != null) return frame;
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
 
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ConsoleFrame frame_ = new ConsoleFrame();
                frame_.init();
                frame = frame_;
            }
        });

        // Wait until frame is created
        while(frame == null)
            try {
                Thread.sleep(30);
            } catch (InterruptedException ex) {}
        return frame;
    } 
    
    
    
    private final ArrayDeque<String> inStack = new ArrayDeque<String>(10);
    private final LinkedList<String> inHistory = new LinkedList<String>();
    private int inHistoryIndex = -1;
    private String inHistoryCurrent = null;
    private JScrollPane consoleOutScroll;
    private JTextArea consoleOut;
    private JTextField consoleIn;
    
    
    private ConsoleFrame() {
        super(FRAME_TITLE);
    }
    
    private void init() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(720, 360));
        try {
            BufferedImage[] icons = new BufferedImage[] {
                    ImageIO.read(getClass().getResource("/roottemplate/chat/client/assets/icon_default.png")),
                    ImageIO.read(getClass().getResource("/roottemplate/chat/client/assets/icon_small.png")),
            };
            setIconImages(Arrays.asList(icons));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent winEvt) {
                inStack.add("#exit"); // Hack for closing
            }
        });
        
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        fixStyle(panel);
        add(panel);

        
        consoleOut = new JTextArea();
        consoleOut.setEditable(false);
        consoleOut.setText("");
        consoleOut.setLineWrap(true);

        fixStyle(consoleOut);
        fixSelectionColor(consoleOut);
        
        consoleOutScroll = new JScrollPane(consoleOut);
        consoleOutScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        fixStyle(consoleOutScroll);
        panel.add(consoleOutScroll, BorderLayout.CENTER);
        
        JPanel inPanel = new JPanel();
        fixStyle(inPanel);
        inPanel.setLayout(new BorderLayout());
        panel.add(inPanel, BorderLayout.SOUTH);
        
        JLabel label = new JLabel("> ");
        label.setVerticalAlignment(SwingConstants.TOP);
        fixStyle(label);
        inPanel.add(label, BorderLayout.WEST);
        
        consoleIn = new JTextField();
        CmdCaret caret = new CmdCaret();
        consoleIn.setCaret(caret);
        consoleIn.setCaretColor(Color.WHITE);
        consoleIn.setDocument(new CharLimitedDocument());
        
        fixStyle(consoleIn);
        fixSelectionColor(consoleIn);
        inPanel.add(consoleIn, BorderLayout.CENTER);
        
        
        consoleOut.addKeyListener(new KeyListener() {
            @Override public void keyPressed(KeyEvent e) {}

            @Override public void keyTyped(KeyEvent e) {
                if(e.getModifiersEx() == 0 || e.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK) {
                    consoleIn.requestFocus();
                    consoleIn.dispatchEvent(e);
                    e.consume();
                }
            }
            @Override public void keyReleased(KeyEvent e) {}
        });
        
        consoleIn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(inStack.size() >= 10)
                    inStack.poll();
                if(inHistory.size() >= 25)
                    inHistory.removeLast();
                String text = consoleIn.getText().trim();
                inStack.add(text);
                inHistory.addFirst(text);

                resetHistory();
                consoleIn.setText("");
            }
        });
        
        consoleIn.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "upKeyPressed");
        consoleIn.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "downKeyPressed");
        consoleIn.getActionMap().put("upKeyPressed", new UpDownKeyAction(true));
        consoleIn.getActionMap().put("downKeyPressed", new UpDownKeyAction(false));

        pack();
        setVisible(true);
        consoleIn.requestFocus();
    }
    
    private void fixStyle(JComponent c) {
        c.setFont(CONSOLE_FONT);
        c.setBackground(Color.BLACK);
        c.setForeground(Color.WHITE);
        c.setBorder(null);
    }
    private void fixSelectionColor(JTextComponent c) {
        c.setSelectionColor(Color.WHITE);
        c.setSelectedTextColor(Color.BLACK);
    }

    public void setMaxMessageLength(int length) {
        ((CharLimitedDocument) consoleIn.getDocument()).setCharsLimit(length);
    }
    public void onNewMessageReceived() {
        if(NOTIFY_ABOUT_NEW_MESSAGES)
            toFront();
    }
    public void setNotifyAboutNewMessages(boolean value) {
        NOTIFY_ABOUT_NEW_MESSAGES = value;
    }
    
    private void resetHistory() {
        inHistoryCurrent = null;
        inHistoryIndex = -1;
    }
    public void removeLastHistoryEntry() {
        inHistory.poll();
    }

    private void updateConsoleCaretPolicy() {
        JScrollBar bar = consoleOutScroll.getVerticalScrollBar();
        DefaultCaret caret = (DefaultCaret) consoleOut.getCaret();
        int updatePolicy = (bar.getValue() == bar.getMaximum() - bar.getVisibleAmount()) ? DefaultCaret.ALWAYS_UPDATE :
                DefaultCaret.NEVER_UPDATE;
        if(updatePolicy != caret.getUpdatePolicy())
            caret.setUpdatePolicy(updatePolicy);
    }
    public void out(String text) {
        updateConsoleCaretPolicy();

        String curText = consoleOut.getText();
        int slice = curText.length() + text.length() - BUFFER_SIZE;
        if(slice > 0) {
            if(curText.length() >= slice) {
                consoleOut.replaceRange("", 0, slice);
            } else {
                slice -= curText.length();
                if(text.length() >= slice)
                    text = text.substring(slice);
                else
                    text = "";
                consoleOut.setText(text);
            }
        }
        consoleOut.append(text);
    }
    public void outln() {
        out("\n");
    }
    public void outln(String text) {
        out(text + "\n");
    }
    
    public String in() throws InterruptedException {
        while(inStack.isEmpty())
            Thread.sleep(30);
        return inStack.poll();
    }
    public String pollIn() {
        return inStack.poll();
    }
    public void exit(int status) {
        outln();
        outln("Нажмите Enter, чтобы выйти . . .");
        inStack.clear();
        try {
            in();
        } catch (InterruptedException ex) {}
        System.exit(status);
    }
    
    
    
    private class UpDownKeyAction extends AbstractAction {
        private final boolean up;
        
        public UpDownKeyAction(boolean up) {
            this.up = up;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if(up) {
                if(inHistoryIndex + 1 < inHistory.size()) {
                    if(inHistoryIndex == -1)
                        inHistoryCurrent = consoleIn.getText();
                    inHistoryIndex++;
                    consoleIn.setText(inHistory.get(inHistoryIndex));
                }
            } else {
                if(inHistoryIndex > -1) {
                    inHistoryIndex--;
                    if(inHistoryIndex == -1)
                        consoleIn.setText(inHistoryCurrent);
                    else
                        consoleIn.setText(inHistory.get(inHistoryIndex));
                }
            }
        }
    }

    private static class CharLimitedDocument extends PlainDocument {
        private int charsLimit = Integer.MAX_VALUE;

        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if(str == null) return;
            int sliceEnd = charsLimit - getLength();
            if(sliceEnd <= str.length()) {
                str = str.substring(0, Math.max(0, sliceEnd));
            }
            super.insertString(offs, str, a);
        }

        public int getCharsLimit() {
            return charsLimit;
        }

        public void setCharsLimit(int charsLimit) {
            this.charsLimit = charsLimit;
        }
    }
    
    public class CmdCaret extends DefaultCaret {
        @Override
        protected synchronized void damage(Rectangle r) {
            if (r == null) return;

            // give values to x,y,width,height (inherited from java.awt.Rectangle)
            x = r.x;
            y = r.y - getComponent().getFontMetrics(getComponent().getFont()).getHeight() + r.height + 2;
            height = r.height;
            // A value for width was probably set by paint(), which we leave alone.
            // But the first call to damage() precedes the first call to paint(), so
            // in this case we must be prepared to set a valid width, or else
            // paint()
            // will receive a bogus clip area and caret will not get drawn properly.
            if (width <= 0)
                width = getComponent().getWidth();

            repaint(); // calls getComponent().repaint(x, y, width, height)
        }

        @Override
        public void paint(Graphics g) {
            JTextComponent comp = getComponent();
            if(comp == null)
                return;

            Rectangle r;
            try {
                r = comp.modelToView(getDot());
                if (r == null)
                    return;
            } catch (BadLocationException e) {
                return;
            }

            if ((x != r.x) || (y != r.y)) {
                // paint() has been called directly, without a previous call to
                // damage(), so do some cleanup. (This happens, for example, when
                // the
                // text component is resized.)
                damage(r);
            }

            g.setColor(comp.getCaretColor());
            g.setXORMode(comp.getBackground()); // do this to draw in XOR mode

            width = g.getFontMetrics().charWidth('m');
            height = 4;
            if (isVisible())
                g.fillRect(r.x, r.y + g.getFontMetrics().getHeight() - height - 2, width, height);
        }
    }
}
