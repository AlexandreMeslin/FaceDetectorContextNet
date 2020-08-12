package br.com.meslin.faceDetector;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import lac.cnclib.sddl.serialization.Serialization;
import lac.cnet.sddl.objects.ApplicationObject;
import lac.cnet.sddl.objects.Message;
import lac.cnet.sddl.objects.PrivateMessage;
import lac.cnet.sddl.udi.core.SddlLayer;
import lac.cnet.sddl.udi.core.UniversalDDSLayerFactory;
import lac.cnet.sddl.udi.core.listener.UDIDataReaderListener;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import br.com.meslin.faceDetector.visual.JCamera;

public class CoreServer implements UDIDataReaderListener<ApplicationObject> {
	private MatOfByte mem;
	/** Contains a frontal face classifier */
	private CascadeClassifier faceDetector;
	/** Matrix of rectangles representing detected faces */
	private MatOfRect faceDetections;

	// Visual objects
	private JPanel jPanel;
	private JCamera jCamera;
	
	/** face-detection data-base file name pointing to haarcascade */
	private final String CASCADE = "haarcascade_frontalface_alt2.xml";
	
	/** OpenSplice SDDL core layer */
	private SddlLayer  core;

	public CoreServer() {
		// Load OpenSplice + ContextNet components
		core = UniversalDDSLayerFactory.getInstance();
		core.createParticipant(UniversalDDSLayerFactory.CNET_DOMAIN);
		core.createPublisher();
		core.createSubscriber();

		Object receiveMessageTopic = core.createTopic(Message.class, Message.class.getSimpleName());
		core.createDataReader(this, receiveMessageTopic);

		Object toMobileNodeTopic = core.createTopic(PrivateMessage.class, PrivateMessage.class.getSimpleName());
		core.createDataWriter(toMobileNodeTopic);

		// Load OpenCV components
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		jCamera = new JCamera();
		mem = new MatOfByte();
		faceDetections = new MatOfRect();

		// to run under Windows, replace 0 by 1 in the next line
		faceDetector = new CascadeClassifier(FaceDetection.class.getResource(CASCADE).getPath().substring(0).replace("%20", " "));
		faceDetector = new CascadeClassifier("/usr/lib/opencv-4.4.0/data/haarcascades/" + CASCADE);
		System.err.println("CoreServer3");

		initComponents();

		System.out.println("=== Server Started (Listening) ===");
	}

	/**
	 * Main function
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getLogger("").setLevel(Level.OFF);

		CoreServer coreServer = new CoreServer();

		/* Create and display the form */
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				coreServer.jCamera.setVisible(true);
			}
		});
		
		synchronized (coreServer) {
			try {
				coreServer.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * On new data received
	 * @param topicSample a sample of a topic
	 */
	@Override
	public void onNewData(ApplicationObject topicSample) {
		Message message = (Message) topicSample;
		
		System.out.println("==> " + Serialization.fromJavaByteStream(message.getContent()));
		
		if(Serialization.fromJavaByteStream(message.getContent()) instanceof String) {
			System.out.println("String ==> " + Serialization.fromJavaByteStream(message.getContent()));
		}
		else {
			Graphics g = jPanel.getGraphics();
			// Here, we have a MatOfByte converted to array (content = mem.toArray())
			byte [] content = message.getContent();
			System.out.println("Outro ==> " + Serialization.fromJavaByteStream(content).getClass().getCanonicalName());
			System.out.println("content = " + content.length);
			
			Image im;
			try {
				im = ImageIO.read(new ByteArrayInputStream((byte[]) Serialization.fromJavaByteStream(content)));
				BufferedImage buff = (BufferedImage) im;
				
				// Convert PNG image to org.opencv.Mat
				// From: https://stackoverflow.com/questions/14958643/converting-bufferedimage-to-mat-in-opencv
				Mat frame = new Mat(buff.getHeight(), buff.getWidth(), CvType.CV_8UC4);
				byte[] data = ((DataBufferByte) buff.getRaster().getDataBuffer()).getData();
				frame.put(0, 0, data);

				// detect faces here
				faceDetector.detectMultiScale(frame, faceDetections);
				for (Rect rect : faceDetections.toArray()) {
					Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255,0));
				}
				Imgcodecs.imencode(".png", frame, mem);
				im = ImageIO.read(new ByteArrayInputStream(mem.toArray()));
//				buff = (BufferedImage) im;	// FIXME: please, put this line back to the program
				
				// set form factor according to frame (frame --> mem --> buff)
				int painelW = jCamera.getWidth();
				int painelH = jCamera.getHeight() - 150;
				
				if(painelW * buff.getHeight() < painelH * buff.getWidth()) {
					jPanel.setBounds(0, 150, painelW, buff.getHeight() * painelW / buff.getWidth());
				}
				else {
					jPanel.setBounds(0, 150, buff.getWidth() * painelH / buff.getHeight(), painelH);
				}
				g.drawImage(buff, 0, 0, jPanel.getSize().width, jPanel.getSize().height, 0, 0, buff.getWidth(), buff.getHeight(), null);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Initialize window layout
	 */
	private void initComponents() {
		jPanel = new JPanel();

		jCamera.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		GroupLayout jPanel1Layout = new GroupLayout(jPanel);
		jPanel1Layout.setHorizontalGroup(
			jPanel1Layout.createParallelGroup(Alignment.LEADING)
				.addGap(0, 711, Short.MAX_VALUE)
		);
		jPanel1Layout.setVerticalGroup(
			jPanel1Layout.createParallelGroup(Alignment.LEADING)
				.addGap(0, 376, Short.MAX_VALUE)
		);
		jPanel.setLayout(jPanel1Layout);
	
		GroupLayout layout = new GroupLayout(jCamera.getContentPane());
		layout.setHorizontalGroup(
			layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(jPanel, GroupLayout.DEFAULT_SIZE, 711, Short.MAX_VALUE)
							.addGap(24))))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addGap(22)
					.addComponent(jPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(43, Short.MAX_VALUE))
		);
		jCamera.getContentPane().setLayout(layout);

		jCamera.pack();
	}
}