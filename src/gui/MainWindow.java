package gui;

import model.SearchFilter;
import model.Studio;
import service.StudioService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private final StudioService service;

    // Контейнер для списка карточек
    private JPanel cardsContainer;
    private JScrollPane scrollPane;

    // Панель деталей (появляется при выборе студии)
    private JPanel detailsPanel;
    private JLabel lblDetailName, lblDetailAddress, lblDetailPrice, lblDetailRating;
    private JTextArea taDescription;

    // Список инструментов ВНУТРИ карточки-details (для просмотра)
    private JList<String> listInstrumentsDetails;
    private DefaultListModel<String> instrumentsModelDetails;

    // --- ЧЕКБОКСЫ ДЛЯ ФИЛЬТРАЦИИ ---
    private List<JCheckBox> instrumentCheckboxes = new ArrayList<>();

    // Элементы фильтрации (цена/рейтинг)
    private JTextField tfMaxPrice;
    private JSpinner spinnerRating;

    // Текущая выбранная студия
    private Studio selectedStudio;

    public MainWindow() {
        setTitle("Поиск репетиционных студий");
        setSize(450, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Настройка шрифтов
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 13));

        this.service = new StudioService();

        initComponents();
        refreshList();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // --- ВЕРХНЯЯ ПАНЕЛЬ ФИЛЬТРОВ ---
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Ряд 1: Инструменты (ЧЕКБОКСЫ)
        JPanel row1 = new JPanel(new BorderLayout(5, 5));
        row1.setBackground(Color.WHITE);
        JLabel lblInst = new JLabel("Инструменты (выберите нужные):");
        lblInst.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblInst.setForeground(Color.DARK_GRAY);
        row1.add(lblInst, BorderLayout.NORTH);

        // Создаем панель для чекбоксов
        JPanel checkboxesPanel = new JPanel();
        checkboxesPanel.setLayout(new BoxLayout(checkboxesPanel, BoxLayout.Y_AXIS));
        checkboxesPanel.setBackground(Color.WHITE);
        checkboxesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] availableInstruments = {
                "Барабаны", "Гитарный комбик", "Бас-гитара",
                "Вокальный тракт", "Клавиши", "Саксофон"
        };

        for (String inst : availableInstruments) {
            JCheckBox cb = new JCheckBox(inst);
            cb.setBackground(Color.WHITE);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            checkboxesPanel.add(cb);
            instrumentCheckboxes.add(cb);
        }

        // Оборачиваем панель чекбоксов в скролл
        JScrollPane scrollFilterInst = new JScrollPane(checkboxesPanel);
        scrollFilterInst.setPreferredSize(new Dimension(0, 90));
        scrollFilterInst.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        row1.add(scrollFilterInst, BorderLayout.CENTER);
        filterPanel.add(row1);
        filterPanel.add(Box.createVerticalStrut(10));

        // Ряд 2: Цена и Рейтинг
        JPanel row2 = new JPanel(new GridLayout(1, 2, 15, 0));
        row2.setBackground(Color.WHITE);

        JPanel priceBlock = new JPanel(new BorderLayout(5, 5));
        priceBlock.setBackground(Color.WHITE);
        JLabel lblPrice = new JLabel("Макс. цена (₽):");
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPrice.setForeground(Color.DARK_GRAY);
        priceBlock.add(lblPrice, BorderLayout.NORTH);
        tfMaxPrice = new JTextField("5000");
        tfMaxPrice.setHorizontalAlignment(JTextField.RIGHT);
        tfMaxPrice.setPreferredSize(new Dimension(0, 32));
        priceBlock.add(tfMaxPrice, BorderLayout.CENTER);
        row2.add(priceBlock);

        JPanel ratingBlock = new JPanel(new BorderLayout(5, 5));
        ratingBlock.setBackground(Color.WHITE);
        JLabel lblRating = new JLabel("Рейтинг от:");
        lblRating.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRating.setForeground(Color.DARK_GRAY);
        ratingBlock.add(lblRating, BorderLayout.NORTH);
        spinnerRating = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 5.0, 0.5));
        spinnerRating.setPreferredSize(new Dimension(0, 32));
        ((JSpinner.DefaultEditor)spinnerRating.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
        ratingBlock.add(spinnerRating, BorderLayout.CENTER);
        row2.add(ratingBlock);

        filterPanel.add(row2);
        filterPanel.add(Box.createVerticalStrut(15));

        // Кнопка Найти
        JButton btnFilter = new JButton("Найти студии");
        btnFilter.setBackground(Color.BLACK);
        btnFilter.setForeground(Color.WHITE);
        btnFilter.setFocusPainted(false);
        btnFilter.setBorderPainted(false);
        btnFilter.setPreferredSize(new Dimension(0, 40));
        btnFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFilter.addActionListener(e -> applyFilter());
        filterPanel.add(btnFilter);

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(Color.LIGHT_GRAY);
        filterPanel.add(Box.createVerticalStrut(10));
        filterPanel.add(separator);

        // --- ЦЕНТРАЛЬНАЯ ЧАСТЬ: СПИСОК + ДЕТАЛИ ---
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(new Color(245, 245, 247));

        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setBackground(new Color(245, 245, 247));

        scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        detailsPanel = createDetailsPanel();
        detailsPanel.setVisible(false);

        centerWrapper.add(scrollPane, BorderLayout.CENTER);
        centerWrapper.add(detailsPanel, BorderLayout.SOUTH);

        // --- НИЖНЯЯ ПАНЕЛЬ ДЕЙСТВИЙ ---
        JPanel actionPanel = new JPanel(new GridLayout(1, 4, 1, 0));
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        actionPanel.setPreferredSize(new Dimension(450, 55));

        JButton btnAdd = createActionBtn("Добавить", Color.DARK_GRAY);
        JButton btnEdit = createActionBtn("Изменить", Color.DARK_GRAY);
        JButton btnDelete = createActionBtn("Удалить", Color.DARK_GRAY);
        JButton btnSaveList = createActionBtn("Сохранить список", new Color(52, 73, 94));

        btnAdd.addActionListener(e -> openAddEditDialog(null));
        btnEdit.addActionListener(e -> editSelectedStudio());
        btnDelete.addActionListener(e -> deleteSelectedStudio());
        btnSaveList.addActionListener(e -> exportCurrentList());

        actionPanel.add(btnAdd);
        actionPanel.add(btnEdit);
        actionPanel.add(btnDelete);
        actionPanel.add(btnSaveList);

        add(filterPanel, BorderLayout.NORTH);
        add(centerWrapper, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }

    // Метод создания панели деталей
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        panel.setMaximumSize(new Dimension(450, 300));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        lblDetailName = new JLabel("");
        lblDetailName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblDetailName.setForeground(Color.BLACK);

        lblDetailAddress = new JLabel("");
        lblDetailAddress.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDetailAddress.setForeground(Color.GRAY);

        JPanel metaPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        metaPanel.setBackground(Color.WHITE);
        lblDetailPrice = new JLabel("");
        lblDetailPrice.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDetailPrice.setForeground(Color.BLACK);

        lblDetailRating = new JLabel("");
        lblDetailRating.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDetailRating.setForeground(Color.GRAY);

        metaPanel.add(lblDetailPrice);
        metaPanel.add(lblDetailRating);

        headerPanel.add(lblDetailName, BorderLayout.NORTH);
        headerPanel.add(lblDetailAddress, BorderLayout.CENTER);
        headerPanel.add(metaPanel, BorderLayout.EAST);

        JLabel lblDescTitle = new JLabel("Описание:");
        lblDescTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDescTitle.setForeground(Color.DARK_GRAY);

        taDescription = new JTextArea(3, 20);
        taDescription.setEditable(false);
        taDescription.setLineWrap(true);
        taDescription.setWrapStyleWord(true);
        taDescription.setBackground(Color.WHITE);
        taDescription.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        taDescription.setBorder(null);

        JLabel lblInstTitle = new JLabel("Оборудование в студии:");
        lblInstTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblInstTitle.setForeground(Color.DARK_GRAY);

        // Инициализация списка для деталей (простой текст, без галочек)
        instrumentsModelDetails = new DefaultListModel<>();
        listInstrumentsDetails = new JList<>(instrumentsModelDetails);
        listInstrumentsDetails.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        listInstrumentsDetails.setBackground(Color.WHITE);
        listInstrumentsDetails.setBorder(null);
        listInstrumentsDetails.setSelectionBackground(Color.LIGHT_GRAY);

        JScrollPane instScroll = new JScrollPane(listInstrumentsDetails);
        instScroll.setPreferredSize(new Dimension(0, 60));
        instScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.add(lblDescTitle);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(taDescription);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(lblInstTitle);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(instScroll);

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JButton createActionBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel createStudioCard(Studio s) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        card.setMaximumSize(new Dimension(450, 80));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);

        JLabel lblName = new JLabel(s.getName());
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblName.setForeground(Color.BLACK);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblAddress = new JLabel(s.getAddress());
        lblAddress.setForeground(Color.GRAY);
        lblAddress.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblAddress.setAlignmentX(Component.LEFT_ALIGNMENT);

        leftPanel.add(lblName);
        leftPanel.add(Box.createVerticalStrut(2));
        leftPanel.add(lblAddress);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        JLabel lblPrice = new JLabel(String.format("%.0f ₽", s.getPricePerHour()));
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPrice.setForeground(Color.BLACK);
        lblPrice.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightPanel.add(lblPrice);

        card.add(leftPanel, BorderLayout.WEST);
        card.add(rightPanel, BorderLayout.EAST);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                selectStudio(s, card);
            }
        });

        return card;
    }

    private void selectStudio(Studio s, JPanel clickedCard) {
        this.selectedStudio = s;

        // Сброс цвета всех карточек
        for (Component c : cardsContainer.getComponents()) {
            c.setBackground(Color.WHITE);
        }
        clickedCard.setBackground(new Color(250, 250, 250));

        lblDetailName.setText(s.getName());
        lblDetailAddress.setText(s.getAddress());
        lblDetailPrice.setText(String.format("%.0f ₽/ч", s.getPricePerHour()));
        lblDetailRating.setText(String.format("%.1f", s.getRating()));
        taDescription.setText(s.getDescription() != null ? s.getDescription() : "Описание отсутствует.");

        // Заполняем список инструментов в деталях
        instrumentsModelDetails.clear();
        if (s.getInstruments() != null) {
            for (var inst : s.getInstruments()) {
                instrumentsModelDetails.addElement(inst.getName());
            }
        }

        detailsPanel.setVisible(true);
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    // ИЗМЕНЕННЫЙ МЕТОД ФИЛЬТРАЦИИ (работает с чекбоксами)
    private void applyFilter() {
        SearchFilter filter = new SearchFilter();

        // Проходим по всем нашим чекбоксам и собираем отмеченные
        for (JCheckBox cb : instrumentCheckboxes) {
            if (cb.isSelected()) {
                filter.addInstrument(cb.getText());
            }
        }

        try {
            filter.setMaxPrice(Double.parseDouble(tfMaxPrice.getText().trim()));
        } catch (NumberFormatException e) {
            filter.setMaxPrice(Double.MAX_VALUE);
        }
        filter.setMinRating((Double) spinnerRating.getValue());

        updateList(service.searchStudios(filter));
        detailsPanel.setVisible(false);
        selectedStudio = null;
    }

    private void refreshList() {
        updateList(service.getAllStudios());
    }

    private void updateList(List<Studio> studios) {
        cardsContainer.removeAll();
        if (studios.isEmpty()) {
            JLabel emptyLabel = new JLabel("Студии не найдены", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            cardsContainer.add(emptyLabel);
        } else {
            for (Studio s : studios) {
                cardsContainer.add(createStudioCard(s));
            }
        }
        cardsContainer.revalidate();
        cardsContainer.repaint();
        scrollPane.getVerticalScrollBar().setValue(0);
    }

    private void openAddEditDialog(Studio studio) {
        AddEditStudioDialog dialog = new AddEditStudioDialog(this, studio);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshList();
            detailsPanel.setVisible(false);
        }
    }

    private void editSelectedStudio() {
        if (selectedStudio != null) {
            openAddEditDialog(selectedStudio);
        } else {
            JOptionPane.showMessageDialog(this, "Выберите студию из списка!");
        }
    }

    private void deleteSelectedStudio() {
        if (selectedStudio != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Удалить студию \"" + selectedStudio.getName() + "\"?",
                    "Подтверждение", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                service.removeStudio(selectedStudio.getId());
                refreshList();
                detailsPanel.setVisible(false);
                selectedStudio = null;
            }
        } else {
            JOptionPane.showMessageDialog(this, "Выберите студию для удаления!");
        }
    }

    private void exportCurrentList() {
        List<Studio> currentStudios = new ArrayList<>();
        for (Component c : cardsContainer.getComponents()) {
            if (c instanceof JPanel) {
                JPanel card = (JPanel) c;
                JPanel leftPanel = (JPanel) card.getComponent(0);
                JLabel nameLbl = (JLabel) leftPanel.getComponent(0);
                String name = nameLbl.getText();

                service.getAllStudios().stream()
                        .filter(s -> s.getName().equals(name))
                        .findFirst()
                        .ifPresent(currentStudios::add);
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить список студий");
        fileChooser.setSelectedFile(new File("studios_list.txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("=== СПИСОК РЕПЕТИЦИОННЫХ СТУДИЙ ===");
                writer.println("Дата формирования: " + LocalDate.now());
                writer.println("-----------------------------------\n");

                if (currentStudios.isEmpty()) {
                    writer.println("Студии не найдены.");
                } else {
                    for (Studio s : currentStudios) {
                        writer.println("Название: " + s.getName());
                        writer.println("Адрес: " + s.getAddress());
                        writer.println("Цена: " + String.format("%.0f ₽/ч", s.getPricePerHour()));
                        writer.println("Рейтинг: " + s.getRating());
                        writer.println("Оборудование: " + s.getInstrumentsAsString());
                        writer.println("Описание: " + (s.getDescription() != null ? s.getDescription() : "-"));
                        writer.println("-----------------------------------");
                    }
                }
                JOptionPane.showMessageDialog(this,
                        "Список успешно сохранен в:\n" + file.getAbsolutePath(),
                        "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Ошибка при сохранении файла:\n" + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}