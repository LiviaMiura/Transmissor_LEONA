package br.inpe.transmissor;

import java.awt.Dimension;
import java.io.IOException;
import java.net.InetAddress;

import javax.media.Codec;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.NoProcessorException;
import javax.media.Owned;
import javax.media.Player;
import javax.media.Processor;
import javax.media.control.QualityControl;
import javax.media.control.TrackControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.rtcp.SourceDescription;

public class Transmissor {

    private MediaLocator locator;
    private String enderecoIP;
    private int portaBase;
    private Processor processor = null;
    private RTPManager mensagensRTP[];
    private DataSource dataOutput = null;

    public Transmissor(MediaLocator locator, String ipAddress, String pb,
            Format format) {
        this.locator = locator;
        this.enderecoIP = ipAddress;
        Integer integer = Integer.valueOf("1235");
        if (integer != null) {
            this.portaBase = integer.intValue();
        }
    }

    public synchronized String start() {
        String result;
        result = createProcessor();
        if (result != null) {
            return result;
        }
        result = createTransmitter();
        if (result != null) {
            processor.close();
            processor = null;
            return result;
        }
        processor.start();
        return null;
    }

    public void stop() {
        synchronized (this) {
            if (processor != null) {
                processor.stop();
                processor.close();
                processor = null;
                for (int i = 0; i < mensagensRTP.length; i++) {
                    mensagensRTP[i].removeTargets("Sessao terminada.");
                    mensagensRTP[i].dispose();
                }
            }
        }
    }

    private String createProcessor() {
        if (locator == null) {
            return "Locator e nulo";
        }
        DataSource ds;
        try {
            ds = javax.media.Manager.createDataSource(locator);
        } catch (Exception e) {
            return "Couldn't create DataSource";
        }
        try {
            processor = javax.media.Manager.createProcessor(ds);
        } catch (NoProcessorException npe) {
            return "Couldn't create processor";
        } catch (IOException ioe) {
            return "IOException creating processor";
        }
        boolean result = waitForState(processor, Processor.Configured);
        if (result == false) {
            return "Couldn't configure processor";
        }
        TrackControl[] tracks = processor.getTrackControls();
        if (tracks == null || tracks.length < 1) {
            return "Couldn't find tracks in processor";
        }
        ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
        processor.setContentDescriptor(cd);
        Format supported[];
        Format chosen;
        boolean atLeastOneTrack = false;
        for (int i = 0; i < tracks.length; i++) {
            Format format = tracks[i].getFormat();
            if (tracks[i].isEnabled()) {
                supported = tracks[i].getSupportedFormats();
                if (supported.length > 0) {
                    if (supported[0] instanceof VideoFormat) {
                        chosen = checkForVideoSizes(tracks[i].getFormat(),
                                supported[0]);
                    } else {
                        chosen = supported[0];
                    }
                    tracks[i].setFormat(chosen);
                    System.err
                            .println("Track " + i + " is set to transmit as:");
                    System.err.println("  " + chosen);
                    atLeastOneTrack = true;
                } else {
                    tracks[i].setEnabled(false);
                }
            } else {
                tracks[i].setEnabled(false);
            }
        }
        if (!atLeastOneTrack) {
            return "Couldn't set any of the tracks to a valid RTP format";
        }
        result = waitForState(processor, Controller.Realized);
        if (result == false) {
            return "Couldn't realize processor";
        }
        setJPEGQuality(processor, 0.5f);
        dataOutput = processor.getDataOutput();
        return null;
    }

    private String createTransmitter() {
        PushBufferDataSource pbds = (PushBufferDataSource) dataOutput;
        PushBufferStream pbss[] = pbds.getStreams();
        mensagensRTP = new RTPManager[pbss.length];
        SessionAddress localAddr, destAddr;
        InetAddress ipAddr;
        SendStream sendStream;
        int port;
        SourceDescription srcDesList[];
        for (int i = 0; i < pbss.length; i++) {
            try {
                mensagensRTP[i] = RTPManager.newInstance();
                port = portaBase + 2 * i;
                ipAddr = InetAddress.getByName(enderecoIP);
                localAddr = new SessionAddress(InetAddress.getLocalHost(), port);
                destAddr = new SessionAddress(ipAddr, port);
                mensagensRTP[i].initialize(localAddr);
                mensagensRTP[i].addTarget(destAddr);
                System.err.println("Created RTP session: " + enderecoIP + " "
                        + port);
                sendStream = mensagensRTP[i].createSendStream(dataOutput, i);
                sendStream.start();
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        return null;
    }

    Format checkForVideoSizes(Format original, Format supported) {
        int largura, altura;
        Dimension size = ((VideoFormat) original).getSize();
        Format jpegFmt = new Format(VideoFormat.JPEG_RTP);
        Format h263Fmt = new Format(VideoFormat.H263_RTP);

        if (supported.matches(jpegFmt)) {
            largura = (size.width % 8 == 0 ? size.width
                    : (int) (size.width / 8) * 8);
            altura = (size.height % 8 == 0 ? size.height
                    : (int) (size.height / 8) * 8);
        } else if (supported.matches(h263Fmt)) {
            if (size.width < 128) {
                largura = 128;
                altura = 96;
            } else if (size.width < 176) {
                largura = 176;
                altura = 144;
            } else {
                largura = 352;
                altura = 288;
            }
        } else {

            return supported;
        }

        return (new VideoFormat(null, new Dimension(largura, altura),
                Format.NOT_SPECIFIED, null, Format.NOT_SPECIFIED))
                .intersects(supported);
    }

    void setJPEGQuality(Player p, float val) {
        Control cs[] = p.getControls();
        QualityControl qc = null;
        VideoFormat formato = new VideoFormat(VideoFormat.JPEG);
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] instanceof QualityControl && cs[i] instanceof Owned) {
                Object objeto = ((Owned) cs[i]).getOwner();
                if (objeto instanceof Codec) {
                    Format fmts[] = ((Codec) objeto)
                            .getSupportedOutputFormats(null);
                    for (int j = 0; j < fmts.length; j++) {
                        if (fmts[j].matches(formato)) {
                            qc = (QualityControl) cs[i];
                            qc.setQuality(val);
                            System.err.println("- Qualidade configurada para " + val
                                    + " em " + qc);
                            break;
                        }
                    }
                }
                if (qc != null) {
                    break;
                }
            }
        }
    }

    private Integer stateLock = new Integer(0);
    private boolean failed = false;

    Integer getStateLock() {
        return stateLock;
    }

    void setFailed() {
        failed = true;
    }

    private synchronized boolean waitForState(Processor p, int state) {
        p.addControllerListener(new StateListener());
        failed = false;
        if (state == Processor.Configured) {
            p.configure();
        } else if (state == Processor.Realized) {
            p.realize();
        }
        while (p.getState() < state && !failed) {
            synchronized (getStateLock()) {
                try {
                    getStateLock().wait();
                } catch (InterruptedException ie) {
                    return false;
                }
            }
        }
        if (failed) {
            return false;
        } else {
            return true;
        }
    }

    class StateListener implements ControllerListener {

        public void controllerUpdate(ControllerEvent ce) {
            if (ce instanceof ControllerClosedEvent) {
                setFailed();
            }
            if (ce instanceof ControllerEvent) {
                synchronized (getStateLock()) {
                    getStateLock().notifyAll();
                }
            }
        }
    }
}
