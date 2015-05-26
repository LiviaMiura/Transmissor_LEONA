package br.inpe.transmissor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class TransmissorGUI extends JFrame {
	public static Transmissor transmissor = null;
	private ThreadRTP threadRTP;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
                        @Override
			public void run() {
				new TransmissorGUI();
			}
		});
	}

	public TransmissorGUI() {
		setTitle("Transmissor");
		JPanel painelServer = new JPanel();
		painelServer
				.setLayout(new BoxLayout(painelServer, BoxLayout.LINE_AXIS));
		JLabel nomePorta = new JLabel("Porta para conexcao RTP: ");
		final JTextField porta = new JTextField();
		painelServer.add(nomePorta);
		painelServer.add(porta);
		JPanel painelBotao = new JPanel();
		painelBotao.setLayout(new BoxLayout(painelBotao, BoxLayout.LINE_AXIS));
		final JButton ligar = new JButton("Ligar");
		ligar.setEnabled(true);
		final JButton desligar = new JButton("Desligar");
		desligar.setEnabled(false);
		ligar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ligar.setEnabled(false);
				desligar.setEnabled(true);
				(threadRTP = new ThreadRTP(porta.getText())).execute();
			}
		});
		desligar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ligar.setEnabled(true);
				desligar.setEnabled(false);
				threadRTP.cancel(true);
				threadRTP = null;
			}
		});
		painelBotao.add(ligar);
		painelBotao.add(desligar);
		getContentPane().add(painelServer, BorderLayout.PAGE_START);
		getContentPane().add(painelBotao, BorderLayout.PAGE_END);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(600, 300);
		setVisible(true);
	}

}
