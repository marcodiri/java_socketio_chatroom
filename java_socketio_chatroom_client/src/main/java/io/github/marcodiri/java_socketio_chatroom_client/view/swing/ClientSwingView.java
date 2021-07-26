package io.github.marcodiri.java_socketio_chatroom_client.view.swing;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import io.github.marcodiri.java_socketio_chatroom_client.ChatroomClient;
import io.github.marcodiri.java_socketio_chatroom_client.model.ClientMessage;
import io.github.marcodiri.java_socketio_chatroom_client.view.ClientView;
import io.github.marcodiri.java_socketio_chatroom_client.view.swing.components.MessageBoard;
import io.github.marcodiri.java_socketio_chatroom_core.model.Message;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JButton;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import javax.swing.JScrollPane;
import javax.swing.Box;
import java.awt.Component;
import javax.swing.JTextPane;
import java.awt.Font;
import java.awt.Color;

@SuppressWarnings("serial")
public class ClientSwingView extends JFrame implements ClientView {

    private JPanel contentPane;
    private JTextField txtMessage;
    private JButton btnConnect;
    private JButton btnDisconnect;
    private JScrollPane scrollPane;
    private MessageBoard msgsBoard;
    private JButton btnSend;

    private transient ChatroomClient client;
    private JLabel lblUsername;
    private JTextField txtUsername;
    private Component verticalStrut;
    private Component verticalStrut_1;
    private Component verticalStrut_2;
    private JTextPane txtErrorMessage;

    /**
     * Create the frame.
     *
     * @param board
     */
    public ClientSwingView(MessageBoard board) {
        setPreferredSize(new Dimension(500, 500));
        setTitle("Socket.io Chatroom");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(100, 100, 500, 500);
        setMinimumSize(new Dimension(300, 300));
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        GridBagLayout gbl_contentPane = new GridBagLayout();
        gbl_contentPane.columnWidths = new int[]{125, 0, 0};
        gbl_contentPane.rowHeights = new int[]{0, 0, 0, 0, 0};
        gbl_contentPane.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_contentPane.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        contentPane.setLayout(gbl_contentPane);

        lblUsername = new JLabel("Username");
        GridBagConstraints gbc_lblUsername = new GridBagConstraints();
        gbc_lblUsername.insets = new Insets(0, 0, 5, 5);
        gbc_lblUsername.gridx = 0;
        gbc_lblUsername.gridy = 0;
        contentPane.add(lblUsername, gbc_lblUsername);

        JLabel lblMessages = new JLabel("Messages");
        GridBagConstraints gbc_lblMessages = new GridBagConstraints();
        gbc_lblMessages.insets = new Insets(0, 0, 5, 0);
        gbc_lblMessages.gridx = 1;
        gbc_lblMessages.gridy = 0;
        contentPane.add(lblMessages, gbc_lblMessages);

        Box verticalBox = Box.createVerticalBox();
        verticalBox.setMinimumSize(new Dimension(112, 0));
        verticalBox.setPreferredSize(new Dimension(112, 0));
        verticalBox.setSize(new Dimension(112, 0));
        verticalBox.setMaximumSize(new Dimension(112, 0));
        GridBagConstraints gbc_verticalBox = new GridBagConstraints();
        gbc_verticalBox.fill = GridBagConstraints.VERTICAL;
        gbc_verticalBox.insets = new Insets(0, 0, 5, 5);
        gbc_verticalBox.gridx = 0;
        gbc_verticalBox.gridy = 1;
        contentPane.add(verticalBox, gbc_verticalBox);

        btnConnect = new JButton("Connect");
        btnConnect.setSize(new Dimension(112, 25));
        btnConnect.setEnabled(false);
        btnConnect.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConnect.setName("btnConnect");
        btnConnect.addActionListener(e -> {
            btnConnect.setEnabled(false);
            txtUsername.setEnabled(false);
            txtErrorMessage.setText("");
            client.connect(txtUsername.getText());
        });

        txtUsername = new JTextField();
        txtUsername.setPreferredSize(new Dimension(112, 19));
        txtUsername.setMinimumSize(new Dimension(112, 19));
        txtUsername.setName("txtUsername");
        KeyAdapter txtUsernameEvents = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                btnConnect.setEnabled(
                        !txtUsername.getText().trim().isEmpty()
                );
            }
        };
        txtUsername.addKeyListener(txtUsernameEvents);
        txtUsername.setMaximumSize(new Dimension(112, 19));
        verticalBox.add(txtUsername);
        txtUsername.setColumns(10);

        verticalStrut = Box.createVerticalStrut(20);
        verticalStrut.setMaximumSize(new Dimension(32767, 10));
        verticalBox.add(verticalStrut);
        btnConnect.setPreferredSize(new Dimension(112, 25));
        btnConnect.setMinimumSize(new Dimension(112, 25));
        btnConnect.setMaximumSize(new Dimension(112, 25));
        verticalBox.add(btnConnect);

        btnDisconnect = new JButton("Disconnect");
        btnDisconnect.setSize(new Dimension(112, 25));
        btnDisconnect.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDisconnect.addActionListener(e -> {
            client.disconnect();
            btnDisconnect.setEnabled(false);
            btnConnect.setEnabled(true);
            txtMessage.setEnabled(false);
            msgsBoard.setEnabled(false);
            msgsBoard.setText("");
            txtUsername.setEnabled(true);
        });

        verticalStrut_1 = Box.createVerticalStrut(20);
        verticalStrut_1.setMaximumSize(new Dimension(32767, 5));
        verticalBox.add(verticalStrut_1);
        btnDisconnect.setName("btnDisconnect");
        verticalBox.add(btnDisconnect);
        btnDisconnect.setEnabled(false);

        verticalStrut_2 = Box.createVerticalStrut(20);
        verticalBox.add(verticalStrut_2);

        txtErrorMessage = new JTextPane();
        txtErrorMessage.setName("txtErrorMessage");
        txtErrorMessage.setMaximumSize(new Dimension(112, 200));
        txtErrorMessage.setPreferredSize(new Dimension(112, 0));
        txtErrorMessage.setMinimumSize(new Dimension(112, 0));
        txtErrorMessage.setEditable(false);
        txtErrorMessage.setForeground(Color.RED);
        txtErrorMessage.setFont(new Font("Dialog", Font.BOLD, 12));
        txtErrorMessage.setOpaque(false);
        verticalBox.add(txtErrorMessage);

        scrollPane = new JScrollPane();
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane.gridx = 1;
        gbc_scrollPane.gridy = 1;
        contentPane.add(scrollPane, gbc_scrollPane);

        msgsBoard = board;
        scrollPane.setViewportView(msgsBoard);
        msgsBoard.setEnabled(false);
        msgsBoard.setEditable(false);
        msgsBoard.setName("msgsTextPane");
        KeyAdapter txtMessageEvents = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                btnSend.setEnabled(
                        !txtMessage.getText().trim().isEmpty()
                );
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (!txtMessage.getText().trim().isEmpty() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ClientMessage msg = new ClientMessage(
                            new Timestamp(System.currentTimeMillis()),
                            txtUsername.getText(),
                            txtMessage.getText()
                    );
                    client.sendMessage(msg);
                    txtMessage.setText("");
                }
            }
        };

        txtMessage = new JTextField();
        txtMessage.addKeyListener(txtMessageEvents);
        txtMessage.setEnabled(false);
        txtMessage.setName("txtMessage");
        GridBagConstraints gbc_txtMessage = new GridBagConstraints();
        gbc_txtMessage.insets = new Insets(0, 0, 5, 0);
        gbc_txtMessage.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtMessage.gridx = 1;
        gbc_txtMessage.gridy = 2;
        contentPane.add(txtMessage, gbc_txtMessage);
        txtMessage.setColumns(10);

        btnSend = new JButton("Send");
        btnSend.addActionListener(e -> {
            ClientMessage msg = new ClientMessage(
                    new Timestamp(System.currentTimeMillis()),
                    txtUsername.getText(),
                    txtMessage.getText()
            );
            client.sendMessage(msg);
            txtMessage.setText("");
            btnSend.setEnabled(false);
        });
        btnSend.setEnabled(false);
        GridBagConstraints gbc_btnSend = new GridBagConstraints();
        gbc_btnSend.gridx = 1;
        gbc_btnSend.gridy = 3;
        contentPane.add(btnSend, gbc_btnSend);
    }

    @Override
    public void addMessage(Message msg) {
        SwingUtilities.invokeLater(() -> msgsBoard.newMessageNotify(msg));
    }

    @Override
    public void roomJoined(String roomName) {
        SwingUtilities.invokeLater(() -> {
            btnDisconnect.setEnabled(true);
            txtMessage.setEnabled(true);
            msgsBoard.setEnabled(true);
        });
    }

    @Override
    public void showError(String errorMsg) {
        SwingUtilities.invokeLater(() -> txtErrorMessage.setText(errorMsg));
    }

    public void setClient(ChatroomClient client) {
        this.client = client;
    }
}
