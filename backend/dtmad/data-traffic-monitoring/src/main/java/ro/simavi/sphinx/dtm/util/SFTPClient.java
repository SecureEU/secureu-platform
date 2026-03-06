package ro.simavi.sphinx.dtm.util;

import com.jcraft.jsch.*;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.model.ToolModel;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

public class SFTPClient {

    private String PATHSEPARATOR = "/";

    private ToolModel toolModel;

    private Session session = null;

    private String privateKeyPath;

    private String username;

    private String password;

    private String host;

    private String uploadDirectory;

    private String sourceDirectory;

    private int port = 22;

    public SFTPClient(ToolModel toolModel){
        this.toolModel = toolModel;
        host = getValue(toolModel.getProperties().get("sftpHost"), host);
        username = getValue(toolModel.getProperties().get("sftpUsername"),username);
        password = getValue(toolModel.getProperties().get("sftpPassword"),password);
        port = Integer.parseInt(getValue(toolModel.getProperties().get("sftpPort"),port));
        privateKeyPath = toolModel.getProperties().get("sftpPrivateKeyPath");
        uploadDirectory = toolModel.getProperties().get("sftpUploadDirectory");
        sourceDirectory = toolModel.getProperties().get("persistDir");
    }

    protected void connect() throws JSchException, SftpException {
        JSch jsch = new JSch();

        //decomenteaza daca se foloseste privatekey
        if (!StringUtils.isEmpty(privateKeyPath)) {
            jsch.addIdentity(privateKeyPath);
        }

        session = jsch.getSession(username, host, port );

        // sftp -vvvv -oKexAlgorithms=diffie-hellman-group14-sha1 -oCiphers=aes256-cbc -oPort=6421 tsftp_msp@opgtw004.publications.europa.eu
        Properties config = new Properties();
        config.put("cipher.s2c", "aes128-ctr,aes128-cbc,3des-ctr,3des-cbc,blowfish-cbc,aes192-ctr,aes192-cbc,aes256-ctr,aes256-cbc");
        config.put("cipher.c2s", "aes128-ctr,aes128-cbc,3des-ctr,3des-cbc,blowfish-cbc,aes192-ctr,aes192-cbc,aes256-ctr,aes256-cbc");
        config.put("kex", "ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha256,diffie-hellman-group-exchange-sha1,diffie-hellman-group1-sha1");
        config.put("CheckCiphers", "aes256-ctr,aes192-ctr,aes128-ctr,aes256-cbc,aes192-cbc,aes128-cbc,3des-ctr,arcfour,arcfour128,arcfour256");

        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig(config);

        // de comentat, dupa ce se foloseste privatekey
        if (StringUtils.isEmpty(privateKeyPath) && !StringUtils.isEmpty(password)) {
            session.setPassword(password);
        }

        session.connect();

        System.out.println("[connect]");

    }

    private void upload(String instanceKey) throws JSchException, SftpException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;

       // File folder = new File(sourceDirectory);

        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = dateFormat.format(date);

        String remoteFolder = uploadDirectory + PATHSEPARATOR + strDate + PATHSEPARATOR + "pcap_files";

        if (!exists(sftpChannel,remoteFolder)){
            sftpChannel.mkdir(remoteFolder);
        }

        uploadFolder(sftpChannel, sourceDirectory, remoteFolder);

        sftpChannel.exit();
    }

    public void uploadRemote(String instanceKey) {
        try {
            connect();
            upload(instanceKey);
            disconnect();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }


    // to remote
    private void uploadFolder(ChannelSftp channelSftp, String sourcePath, String destinationUploadDirectory) throws SftpException {

        final File folder = new File(sourcePath);

        for (final File f : folder.listFiles()) {

            if (f.isFile()) {

                String destFile = destinationUploadDirectory + PATHSEPARATOR + f.getName();

                channelSftp.put(sourcePath + PATHSEPARATOR + f.getName(), destFile);

                System.out.println(f.getName());
            }

            if (f.isDirectory()) {

                String destFile = destinationUploadDirectory + PATHSEPARATOR + f.getName();

                // create directory
                if (!exists(channelSftp,destFile)){
                    channelSftp.mkdir(destFile);
                }


                uploadFolder(channelSftp, sourcePath + PATHSEPARATOR + f.getName(), destinationUploadDirectory  + PATHSEPARATOR + f.getName());
            }

        }

    }

    private boolean exists(ChannelSftp channelSftp, String path) {
        Vector res = null;
        try {
            res = channelSftp.ls(path);
        } catch (SftpException e) {
            if (e.id == SSH_FX_NO_SUCH_FILE) {
                return false;
            }
        }
        return res != null && !res.isEmpty();
    }

    protected void disconnect() {
        if (session != null) {
            session.disconnect();
        }
    }

    private String getValue(String value, String defaultValue){
        if (value==null){
            return defaultValue;
        }
        return value;
    }

    private String getValue(String value, Integer defaultValue){
        if (value==null){
            return defaultValue.toString();
        }
        return value;
    }

}
