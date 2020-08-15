/**
 * 
 */
package br.com.meslin.faceDetector;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.opencv.core.Core;
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
import br.com.meslin.util.Debug;

/**
 * @author meslin
 *
 */
public class TCPFaceDetector {
    /** Contextnet Gateway TCP port number */
    private final static int gatewayPort = 5500;

    // OpenCV objects
    /** a video frame */
	private Mat frame;
	private MatOfByte mem;
	/** Contains a frontal face classifier */
	private CascadeClassifier faceDetector;
	/** face-detection data-base file name pointing to haarcascade */
	private final String CASCADE = "haarcascade_frontalface_alt2.xml";
	/** Matrix of rectangles representing detected faces */
	private MatOfRect faceDetections;

	// Visual objects
	private JPanel jPanel;
	private JCamera jCamera;

	/**
	 * 
	 */
	public TCPFaceDetector() {
		// Load OpenCV components
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		jCamera = new JCamera();
		mem = new MatOfByte();
		
		faceDetections = new MatOfRect();
		// to run under Windows, replace 0 by 1 in the next line
		faceDetector = new CascadeClassifier(FaceDetection.class.getResource(CASCADE).getPath().substring(0).replace("%20", " "));
		initComponents();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TCPFaceDetector thisMain = new TCPFaceDetector();
		thisMain.doAll();
	}
	
	private void doAll() {
		/* Create and display the form */
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				jCamera.setVisible(true);
			}
		});
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(gatewayPort);
			System.out.println("READY!!!");
			while(true) {
				new ClientHandler(serverSocket.accept()).start();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
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
	
	/**
	 * 
	 * @author meslin
	 *
	 */
	class ClientHandler extends Thread {
		private Socket clientSocket;
		private DataInputStream in;
		
		/**
		 * 
		 * @param socket
		 */
		public ClientHandler(Socket socket) {
			this.clientSocket = socket;
		}
		
		/**
		 * Reads the frames
		 */
		public void run() {
			int cols = 0;
			int rows = 0;
			int elemSize = 0;
			int type = 0;
			/** number of bytes read from the stream */
			int qtdReadParcial;
			int qtdReadTotal;
			ByteBuffer buffer = ByteBuffer.allocate(8000*8000 *4); 	// 8k image size using 4-byte pixel
            byte[] temp = new byte[8000*8000 *4];
            int mtu = 1000;
			
			try {
	            in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
	            qtdReadTotal = 0;
	            while(true) {
	            	readSync();
		            // read cols, rows, elemSize, and type
		            cols = in.readInt();
		            rows = in.readInt();
		            elemSize = in.readInt();
		            type = in.readInt();
					int totalToRead = in.readInt();
		            int offset = in.readInt();
	            	qtdReadParcial = in.read(temp, 0, Math.min(mtu, totalToRead));
	            	qtdReadTotal += qtdReadParcial;
	            	buffer.position(offset);
	            	buffer.put(temp, 0, qtdReadParcial);
		            frame = matFromByte(buffer, rows, cols, elemSize, type);
	            	onNewFrame(frame);
	            }
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Reads the sync preamble
		 * @throws IOException
		 */
		private void readSync() throws IOException {
			int n = 0;
			byte sync;

			while(n<10) {
				sync = in.readByte();
				if(n%2==0) {
					if(sync == 0) {
						n++;
					}
					else {
						n = 0;
					}
				}
				else {
					if(sync == (byte)0xFF) {
						n++;
					}
					else if(sync == 0) {
						n = 1;
					}
					else {
						n = 0;
					}
				}
			}
		}

		/**
		 * Creates a Mat from a ByteBuffer
		 * @param buffer
		 * @param rows
		 * @param cols
		 * @param type
		 * @return
		 */
		private Mat matFromByte(ByteBuffer buffer, int rows, int cols, int elemSize, int type) {
	        Mat mat = new Mat(rows, cols, type);
	        byte[] bytes = new byte[rows * cols * elemSize];
	        buffer.position(0);
	        buffer.get(bytes, 0, rows * cols * elemSize);
	        mat.put(0, 0, bytes);       
            return mat;
			
		}
	    /**
	     * Creates a Mat from a byte[]
	     * From: https://stackoverflow.com/questions/27065062/opencv-mat-object-serialization-in-java
	     * @param byteArray
	     * @param rows
	     * @param cols
	     * @param type
	     * @return a Mat object
	     */
/*		private Mat matFromByte(byte[] byteArray, int rows, int cols, int type) {
	        Mat mat = new Mat(rows, cols, type);
	        mat.put(0, 0, byteArray);
	        return mat;
		}
*/	    /**
	     * Creates a Mat from a List<Byte>
	     * From: https://stackoverflow.com/questions/27065062/opencv-mat-object-serialization-in-java
	     * @param dataList
	     * @param rows
	     * @param cols
	     * @param type
	     * @return a Mat object
	     */
/*	    private Mat matFromByte(List<Byte> dataList, int rows, int cols, int type){
	        Mat mat = new Mat(rows, cols, type);

	        Byte[] data = new Byte[dataList.size()];
	        data = dataList.toArray(data);
	        // From: https://stackoverflow.com/questions/12944377/how-to-convert-byte-to-byte-and-the-other-way-around
	        mat.put(0, 0, ArrayUtils.toPrimitive(data));

	        return mat;
	    }
*/
		/**
		 * On new data received
		 * @param topicSample a sample of a topic
		 */
		public void onNewFrame(Mat frame) {
			// From: https://sites.google.com/site/pdopencvjava/mat-to-java-bufferedimage
			MatOfByte mob = new MatOfByte();
			Imgcodecs.imencode(".bmp", frame, mob);
			byte[] byteArray = mob.toArray();
			BufferedImage bufImage = null;
			InputStream in = new ByteArrayInputStream(byteArray);
			try {
				bufImage = ImageIO.read(in);
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Graphics g = jPanel.getGraphics();
			g.drawImage(bufImage, 0, 0, jPanel.getSize().width, jPanel.getSize().height, 0, 0, bufImage.getWidth(), bufImage.getHeight(), null);

/*
 			try {
				// detect faces here
				faceDetector.detectMultiScale(frame, faceDetections);
				for (Rect rect : faceDetections.toArray()) {
					Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255,0));
				}
				Imgcodecs.imencode(".bmp", frame, mem);
				BufferedImage im = ImageIO.read(new ByteArrayInputStream(mem.toArray()));
				//				buff = (BufferedImage) im;	// FIXME: please, put this line back to the program

				// set form factor according to frame (frame --> mem --> buff)
				int painelW = jCamera.getWidth();
				int painelH = jCamera.getHeight() - 150;

				if(painelW * im.getHeight() < painelH * im.getWidth()) {
					jPanel.setBounds(0, 150, painelW, im.getHeight() * painelW / im.getWidth());
				}
				else {
					jPanel.setBounds(0, 150, im.getWidth() * painelH / im.getHeight(), painelH);
				}
				g.drawImage(im, 0, 0, jPanel.getSize().width, jPanel.getSize().height, 0, 0, im.getWidth(), im.getHeight(), null);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
*/		}
	}
	/**
	 * 
	 * @author meslin
	 * 
	 */
	class DaemonThread implements Runnable {
		protected volatile boolean runnable = false;

		@Override
		public void run() {
			synchronized (this) {
				while (runnable) {
					// grab a frame from camera
					if (frame != null) {
						try {
							// retrieve a previously grabbed frame and decode it
							Graphics g = jPanel.getGraphics();
							faceDetector.detectMultiScale(frame, faceDetections);
							for (Rect rect : faceDetections.toArray()) {
								Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255,0));
							}
							//Highgui.imencode(".bmp", frame, mem);
							Imgcodecs.imencode(".png", frame, mem);
							Image im = ImageIO.read(new ByteArrayInputStream(mem.toArray()));
							BufferedImage buff = (BufferedImage) im;
							
							// set form factor according to frame (frame --> mem --> buff)
							int painelW = jCamera.getWidth();
							int painelH = jCamera.getHeight() - 150;
							
							if(painelW * buff.getHeight() < painelH * buff.getWidth()) {
								jPanel.setBounds(0, 150, painelW, buff.getHeight() * painelW / buff.getWidth());
							}
							else {
								jPanel.setBounds(0, 150, buff.getWidth() * painelH / buff.getHeight(), painelH);
							}
							
							if (g.drawImage(buff, 0, 0, jPanel.getSize().width, jPanel.getSize().height, 0, 0, buff.getWidth(), buff.getHeight(), null)) {
								if (runnable == false) {
									System.out.println("Paused ..... ");
									this.wait();
								}
							}
						} catch (Exception ex) {
							Debug.error("Error!!", ex);
							ex.printStackTrace();
						}
					}
				}
			}
		}
	}
}
