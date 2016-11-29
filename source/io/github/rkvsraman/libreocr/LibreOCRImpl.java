package io.github.rkvsraman.libreocr;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

import com.sun.star.lib.uno.helper.Factory;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SpinnerNumberModel;

import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.task.XJobExecutor;
import com.sun.star.lib.uno.helper.WeakBase;

public final class LibreOCRImpl extends WeakBase implements com.sun.star.lang.XServiceInfo, XJobExecutor

{
	private final XComponentContext m_xContext;
	private static final String m_implementationName = LibreOCRImpl.class.getName();
	private static final String[] m_serviceNames = { "io.github.rkvsraman.LibreOCR" };
	String serverName = "";
	JDialog dialog = null;
	Properties langProperties = new Properties();

	public LibreOCRImpl(XComponentContext context) {
		m_xContext = context;
		try {
			langProperties.load(LibreOCRImpl.class.getResourceAsStream("lang.properties"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	};

	public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
		XSingleComponentFactory xFactory = null;

		if (sImplementationName.equals(m_implementationName))
			xFactory = Factory.createComponentFactory(LibreOCRImpl.class, m_serviceNames);
		return xFactory;
	}

	public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
		return Factory.writeRegistryServiceInfo(m_implementationName, m_serviceNames, xRegistryKey);
	}

	// com.sun.star.lang.XServiceInfo:
	public String getImplementationName() {
		return m_implementationName;
	}

	public boolean supportsService(String sService) {
		int len = m_serviceNames.length;

		for (int i = 0; i < len; i++) {
			if (sService.equals(m_serviceNames[i]))
				return true;
		}
		return false;
	}

	public String[] getSupportedServiceNames() {
		return m_serviceNames;
	}

	@Override
	public void trigger(String actionNames) {
		System.out.println("Actions is :" + actionNames);

		Preferences pref = Preferences.userNodeForPackage(LibreOCRImpl.class);
		serverName = pref.get("serverName", "35.164.84.230");

		dialog = new JDialog();
		dialog.setTitle("Upload OCR Document");
		dialog.getContentPane().setLayout(new GridLayout(5, 2));

		JTextField serverAddress = new JTextField(serverName);
		JButton fileButton = new JButton("Choose File");
		JButton okButton = new JButton("Upload");

		JFileChooser fileChooser = new JFileChooser();
		fileButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				int i = fileChooser.showOpenDialog(dialog);
				if (i == JFileChooser.APPROVE_OPTION) {
					fileButton.setText(fileChooser.getSelectedFile().getName());
				}

			}
		});

		JComboBox<String> langList = new JComboBox<String>();
		DefaultComboBoxModel<String> listModel = new DefaultComboBoxModel<String>();
		for (String s : langProperties.stringPropertyNames()) {
			listModel.addElement(s);
		}

		langList.setModel(listModel);
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(300, 75, 600, 25));
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(false);
		progressBar.setVisible(true);
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				pref.put("serverName", serverAddress.getText());
				try {
					pref.flush();

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				progressBar.setIndeterminate(true);
				okButton.setText("Uploading....");

				SpinnerNumberModel model = (SpinnerNumberModel)spinner.getModel();
				
				doUpload(fileChooser.getSelectedFile(), listModel.getElementAt(langList.getSelectedIndex()) , model.getNumber().intValue());

			}
		});
	

		dialog.getContentPane().add(new JLabel("Server Address:"));
		dialog.getContentPane().add(serverAddress);
		dialog.getContentPane().add(new JLabel("File to Upload:"));
		dialog.getContentPane().add(fileButton);
		dialog.getContentPane().add(new JLabel("Select Language:"));
		dialog.getContentPane().add(langList);
		dialog.getContentPane().add(new JLabel("Image DPI:"));
		dialog.getContentPane().add(spinner);
		dialog.getContentPane().add(okButton);
		dialog.getContentPane().add(progressBar);

		dialog.setSize(400, 200);

		dialog.setLocationByPlatform(true);
		dialog.setVisible(true);
	}

	protected void doUpload(File selectedFile, String elementAt, int spinnervalue) {
		// TODO Auto-generated method stub
		HttpRequest httpRequest = HttpRequest.post("http://" + serverName + ":8081/ocr").form("lang",
				langProperties.getProperty(elementAt),"dpi",String.valueOf(spinnervalue),"myfile", selectedFile);
		HttpResponse response = httpRequest.send();

		byte[] responseBytes = response.bodyBytes();
		dialog.setVisible(false);

		try {
			File outputFile = File.createTempFile("down", ".odt");

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
			out.write(responseBytes);
			out.flush();
			out.close();

			com.sun.star.lang.XMultiComponentFactory xMCF = m_xContext.getServiceManager();

			Object oDesktop = xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", m_xContext);

			com.sun.star.frame.XComponentLoader xCompLoader = UnoRuntime
					.queryInterface(com.sun.star.frame.XComponentLoader.class, oDesktop);
			String sUrl = "";
			StringBuffer sbTmp = new StringBuffer("file:///");
			sbTmp.append(outputFile.getCanonicalPath().replace('\\', '/'));
			sUrl = sbTmp.toString();
			xCompLoader.loadComponentFromURL(sUrl, "_blank", 0, new com.sun.star.beans.PropertyValue[0]);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
