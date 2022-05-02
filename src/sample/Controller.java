package sample;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    String useDate = "";

    @FXML public ComboBox<String> comboBoxObjects;
    @FXML public Button startBtn;
    @FXML public Button recordToTapeBtn;
    @FXML public Button checkFilesOnTapeBtn;
    @FXML public Button deleteFilesBtn;
    @FXML public TextArea logText;
    @FXML public GridPane gridPane2;
    @FXML public GridPane gridPane1;
    @FXML public PasswordField password;
    @FXML public DatePicker date;
    @FXML public Label recordToTapeLabel;
    @FXML public Label checkFilesOnTapeLabel;
    @FXML public Label deleteFilesLabel;
    @FXML public Label ip_source;
    @FXML public Label ip_target;

    @FXML
    public void startBtn_onClickMethod() {
        if (password.getText().isEmpty()) {
            logText.appendText("\nPlease, input pass of the user bi2adm");
        } else {
            gridPane1.setDisable(true);
            pushCommand(ip_source.getText(), "find /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/ -type f -name '*" + comboBoxObjects.getValue()
                    + "*" + useDate + "*' -printf \"%f\\n\" > /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
            logText.appendText("\nFilelist /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list created");
            pushCommand(ip_source.getText(), "rsync --progress /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue()
                    + "_arch_list bi2adm@" + ip_target.getText() + ":/usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "/");
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    pushCommand(ip_source.getText(), "while read line; do rsync --progress /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/\"$line\" bi2adm@" + ip_target.getText()
                            + ":/usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "/; done < /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
                    logText.appendText("\nFiles from the list /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list are copied to the archive server");
                    recordToTapeBtn.setDisable(false);
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    @FXML
    public void recordToTape_onClickMethod() {
        recordToTapeBtn.setDisable(true);
        recordToTapeLabel.setDisable(true);
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                pushCommand(ip_target.getText(), "cd /usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "; dsmc ar -servername=p570_bi2_ta -deletefiles -filelist=/usr/sap/trans/ARCHIVE/"
                        + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
                logText.appendText("\nFiles from the list /usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list are recorded to the tape\n");
                checkFilesOnTapeBtn.setDisable(false);
                checkFilesOnTapeLabel.setDisable(false);
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    public void checkFilesOnTape_onClickMethod() {
        checkFilesOnTapeBtn.setDisable(true);
        checkFilesOnTapeLabel.setDisable(true);
        pushCommand(ip_target.getText(), "cd /usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "; dsmc q ar -servername=p570_bi2_ta -filelist=/usr/sap/trans/ARCHIVE/"
                + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
        deleteFilesBtn.setDisable(false);
        deleteFilesLabel.setDisable(false);
    }

    @FXML
    public void deleteFiles_onClickMethod() {
        Alert alert = new Alert(Alert.AlertType.WARNING, "DELETE files from the HEC-server? \nPlease, make sure that the files recorded to the tape. \nFilelist for delete in /D/BI2/ARCHIVE/"
                + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            deleteFilesBtn.setDisable(true);
            deleteFilesLabel.setDisable(true);
            pushCommand(ip_source.getText(), "cd /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "; for f in `cat /D/BI2/ARCHIVE/" + comboBoxObjects.getValue()
                    + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list" + "`; do rm $f; done;");
            logText.appendText("\nFiles from the list /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list are deleted from the HEC");
        }
    }

    @FXML
    public void comboChanged() {
        recordToTapeLabel.setText("dsmc ar -servername=p570_bi2_ta -deletefiles -filelist=/usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
        checkFilesOnTapeLabel.setText("dsmc q ar -servername=p570_bi2_ta -filelist=/usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
        deleteFilesLabel.setText("for f in `cat /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list" + "`; do rm $f; done;");
    }

    @FXML
    public void dateChanged() {
        DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
        LocalDate dateF = date.getValue();
        useDate = formatter.format(dateF).toString();
        recordToTapeLabel.setText("dsmc ar -servername=p570_bi2_ta -deletefiles -filelist=/usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
        checkFilesOnTapeLabel.setText("dsmc q ar -servername=p570_bi2_ta -filelist=/usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
        deleteFilesLabel.setText("for f in `cat /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list" + "`; do rm $f; done;");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        date.setValue(LocalDate.now());
        DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
        LocalDate dateF = date.getValue();
        useDate = formatter.format(dateF).toString();
        recordToTapeLabel.setText("dsmc ar -servername=p570_bi2_ta -deletefiles -filelist=/usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
        checkFilesOnTapeLabel.setText("dsmc q ar -servername=p570_bi2_ta -filelist=/usr/sap/trans/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list");
        deleteFilesLabel.setText("for f in `cat /D/BI2/ARCHIVE/" + comboBoxObjects.getValue() + "/" + useDate + "_" + comboBoxObjects.getValue() + "_arch_list" + "`; do rm $f; done;");
    }

    public void pushCommand(String server, String command_str) {
        try {
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            Session session = jsch.getSession("bi2adm", server, 22);
            session.setPassword(password.getText());
            session.setConfig(config);
            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command_str);
            InputStream in = channel.getInputStream();
            channel.connect();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    logText.appendText("\n" + new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    break;
                }
            }
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {e.printStackTrace(); }
    }
}
