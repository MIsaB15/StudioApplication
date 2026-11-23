package gui;

import model.Instrument;
import model.Studio;
import service.StudioService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class AddEditStudioDialog extends JDialog {
    private final StudioService service;
    private JTextField tfName, tfAddress, tfPrice, tfRating, tfDesc;
    private List<JCheckBox> cbInstruments;
    private boolean saved = false;
    private Studio editingStudio;

    public AddEditStudioDialog(Frame parent, Studio studio) {
        super(parent, studio == null ? "Добавить студию" : "Редактировать студию", true);
        this.editingStudio = studio;
        this.service = new StudioService();
        setSize(500, 500);
        setLocationRelativeTo(parent);
        initComponents();

        if (studio != null) {
            tfName.setText(studio.getName());
            tfAddress.setText(studio.getAddress());
            tfPrice.setText(String.valueOf(studio.getPricePerHour()));
            tfRating.setText(String.valueOf(studio.getRating()));
            tfDesc.setText(studio.getDescription());

            for (JCheckBox cb : cbInstruments) {
                for (Instrument inst : studio.getInstruments()) {
                    if (cb.getText().equals(inst.getName())) {
                        cb.setSelected(true);
                        break;
                    }
                }
            }
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 5, 5));

        formPanel.add(new JLabel("Название:"));
        tfName = new JTextField();
        formPanel.add(tfName);

        formPanel.add(new JLabel("Адрес:"));
        tfAddress = new JTextField();
        formPanel.add(tfAddress);

        formPanel.add(new JLabel("Цена за час (₽):"));
        tfPrice = new JTextField();
        formPanel.add(tfPrice);

        formPanel.add(new JLabel("Рейтинг (0-5):"));
        tfRating = new JTextField();
        formPanel.add(tfRating);

        formPanel.add(new JLabel("Описание:"));
        tfDesc = new JTextField();
        formPanel.add(tfDesc);

        formPanel.add(new JLabel("Инструменты:"));
        JPanel instPanel = new JPanel(new GridLayout(0, 2));
        cbInstruments = new ArrayList<>();
        for (String instName : Instrument.getAllInstruments()) {
            JCheckBox cb = new JCheckBox(instName);
            cbInstruments.add(cb);
            instPanel.add(cb);
        }
        formPanel.add(instPanel);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Сохранить");
        JButton btnCancel = new JButton("Отмена");

        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { saveStudio(); }
        });

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { dispose(); }
        });

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        add(formPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void saveStudio() {
        try {
            String name = tfName.getText().trim();
            String address = tfAddress.getText().trim();
            double price = Double.parseDouble(tfPrice.getText().trim());
            double rating = Double.parseDouble(tfRating.getText().trim());
            String desc = tfDesc.getText().trim();

            List<Instrument> instruments = new ArrayList<>();
            for (JCheckBox cb : cbInstruments) {
                if (cb.isSelected()) {
                    instruments.add(new Instrument(cb.getText()));
                }
            }

            Studio studio;
            if (editingStudio == null) {
                studio = new Studio(name, address, price, rating, instruments, desc);
                service.addStudio(studio);
            } else {
                editingStudio.setName(name);
                editingStudio.setAddress(address);
                editingStudio.setPricePerHour(price);
                editingStudio.setRating(rating);
                editingStudio.setDescription(desc);
                editingStudio.setInstruments(instruments);
                service.updateStudio(editingStudio);
            }

            saved = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Цена и рейтинг должны быть числами!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка валидации", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }
}