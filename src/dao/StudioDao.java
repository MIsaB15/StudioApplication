package dao;

import model.SearchFilter;
import model.Studio;
import java.util.List;

public interface StudioDao {
    List<Studio> findAll();
    void save(Studio studio);
    void update(Studio studio);
    void delete(int id);
    List<Studio> findByFilter(SearchFilter filter);
}