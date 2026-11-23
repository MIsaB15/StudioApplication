package service;

import dao.StudioDao;
import dao.StudioDaoImpl;
import model.SearchFilter;
import model.Studio;

import java.io.IOException; // <-- ДОБАВЬ ЭТУ СТРОКУ
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

public class StudioService {
    private final StudioDao dao;

    public StudioService() {
        this.dao = new StudioDaoImpl();
    }

    public List<Studio> searchStudios(SearchFilter filter) {
        return dao.findByFilter(filter);
    }

    public void addStudio(Studio studio) {
        validateStudio(studio);
        dao.save(studio);
    }

    public void updateStudio(Studio studio) {
        validateStudio(studio);
        dao.update(studio);
    }

    public void removeStudio(int id) {
        dao.delete(id);
    }

    public List<Studio> getAllStudios() {
        return dao.findAll();
    }

    // Метод экспорта списка студий в текстовый файл
    public void exportStudiosToFile(List<Studio> studios, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            writer.println("=== СПИСОК РЕПЕТИЦИОННЫХ СТУДИЙ ===");
            writer.println("Дата формирования: " + LocalDate.now());
            writer.println("-----------------------------------\n");

            if (studios.isEmpty()) {
                writer.println("Студии не найдены.");
            } else {
                for (Studio s : studios) {
                    writer.println("Название: " + s.getName());
                    writer.println("Адрес: " + s.getAddress());
                    writer.println("Цена: " + String.format("%.0f ₽/ч", s.getPricePerHour()));
                    writer.println("Рейтинг: " + s.getRating());
                    writer.println("Оборудование: " + s.getInstrumentsAsString());
                    writer.println("Описание: " + (s.getDescription() != null ? s.getDescription() : "-"));
                    writer.println("-----------------------------------");
                }
            }
        }
    }

    private void validateStudio(Studio studio) {
        if (studio.getName() == null || studio.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Название студии не может быть пустым");
        }
        if (studio.getPricePerHour() <= 0) {
            throw new IllegalArgumentException("Цена должна быть больше нуля");
        }
        if (studio.getRating() < 0 || studio.getRating() > 5) {
            throw new IllegalArgumentException("Рейтинг должен быть от 0 до 5");
        }
    }
}