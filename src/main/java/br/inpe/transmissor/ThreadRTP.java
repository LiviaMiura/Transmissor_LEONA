package br.inpe.transmissor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.format.VideoFormat;
import javax.swing.SwingWorker;

public class ThreadRTP extends SwingWorker<Void, Void> {

    private boolean mensagem = false;
    private String ipCliente = null;
    private Transmissor transmissor;
    private boolean criou = false;
    private String porta;

    public ThreadRTP(String porta) {
        super();
        this.porta = porta;

    }

    @Override
    protected Void doInBackground() {
        while (!isCancelled()) {
            if (!mensagem) {
                mensagem = !mensagem;
                System.err.println("Esperando conexao de um cliente...");
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            if (ipCliente == null) {
                System.err.println("IPCliente is null...");
                try {
                    ServerSocket hostServer = new ServerSocket(1234);
                    Socket socket = hostServer.accept();
                    if (socket.getInetAddress() != null
                            && !socket.getInetAddress().equals("")) {
                        ipCliente = socket.getInetAddress().getHostAddress();
                        criou = true;
                    }
                } catch (IOException e) {
                }
            }
            if (criou) {
                System.err.println("IPCliente is :  " + criou);
                System.err.println("IPCliente is :  " + ipCliente);
                System.err.println("Porta is :  " + porta);
                criou = false;
                transmissor = new Transmissor(new MediaLocator("vfw://0"),
                        ipCliente, porta, new Format(VideoFormat.JPEG));
                String result = transmissor.start();
                if (result != null) {
                    System.err.println("Error : " + result);
                }
                System.err
                        .println("Transmissao RTP iniciada para " + ipCliente);
            }
        }
        if (transmissor != null) {
            transmissor.stop();
        }
        System.err.println("Servidor desligado.");
        return null;
    }

    public String getIpCliente() {
        return ipCliente;
    }

    public void setIpCliente(String ipCliente) {
        this.ipCliente = ipCliente;
    }

}
