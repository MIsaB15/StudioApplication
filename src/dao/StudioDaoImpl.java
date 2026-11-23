package dao;

import model.Instrument;
import model.SearchFilter;
import model.Studio;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudioDaoImpl implements StudioDao {
    private static final String URL = "jdbc:sqlite:studios_database.db";

    public StudioDaoImpl() { initDatabase(); }

    private void initDatabase() {
        String sqlStudios = "CREATE TABLE IF NOT EXISTS studios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, address TEXT NOT NULL, " +
                "price REAL NOT NULL, rating REAL NOT NULL, " +
                "description TEXT);";

        String sqlInstruments = "CREATE TABLE IF NOT EXISTS instruments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE);";

        String sqlLink = "CREATE TABLE IF NOT EXISTS studio_instruments (" +
                "studio_id INTEGER NOT NULL, instrument_id INTEGER NOT NULL, " +
                "PRIMARY KEY (studio_id, instrument_id), " +
                "FOREIGN KEY(studio_id) REFERENCES studios(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(instrument_id) REFERENCES instruments(id));";

        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlStudios);
            stmt.execute(sqlInstruments);
            stmt.execute(sqlLink);

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM instruments");
            if (rs.next() && rs.getInt(1) == 0) {
                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO instruments(name) VALUES(?)");
                for (String inst : Instrument.getAllInstruments()) {
                    pstmt.setString(1, inst);
                    pstmt.executeUpdate();
                }
                pstmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка создания БД: " + e.getMessage());
        }
    }

    @Override
    public List<Studio> findAll() {
        List<Studio> list = new ArrayList<>();
        String sql = "SELECT s.*, GROUP_CONCAT(i.name) as inst_names FROM studios s " +
                "LEFT JOIN studio_instruments si ON s.id = si.studio_id " +
                "LEFT JOIN instruments i ON si.instrument_id = i.id " +
                "GROUP BY s.id";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRowToStudio(rs));
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void save(Studio studio) {
        String sqlStudio = "INSERT INTO studios(name, address, price, rating, description) VALUES(?,?,?,?,?)";
        String sqlInstId = "SELECT id FROM instruments WHERE name = ?";
        String sqlLink = "INSERT INTO studio_instruments(studio_id, instrument_id) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement pstmtStudio = conn.prepareStatement(sqlStudio, Statement.RETURN_GENERATED_KEYS);
                pstmtStudio.setString(1, studio.getName());
                pstmtStudio.setString(2, studio.getAddress());
                pstmtStudio.setDouble(3, studio.getPricePerHour());
                pstmtStudio.setDouble(4, studio.getRating());
                pstmtStudio.setString(5, studio.getDescription());
                pstmtStudio.executeUpdate();

                ResultSet keys = pstmtStudio.getGeneratedKeys();
                int studioId = keys.next() ? keys.getInt(1) : 0;
                studio.setId(studioId);

                PreparedStatement pstmtInst = conn.prepareStatement(sqlInstId);
                PreparedStatement pstmtLink = conn.prepareStatement(sqlLink);

                for (Instrument inst : studio.getInstruments()) {
                    pstmtInst.setString(1, inst.getName());
                    ResultSet rsInst = pstmtInst.executeQuery();
                    if (rsInst.next()) {
                        pstmtLink.setInt(1, studioId);
                        pstmtLink.setInt(2, rsInst.getInt("id"));
                        pstmtLink.executeUpdate();
                    }
                    rsInst.close();
                }
                pstmtInst.close(); pstmtLink.close(); pstmtStudio.close();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    @Override
    public void update(Studio studio) {
        String sqlDelLinks = "DELETE FROM studio_instruments WHERE studio_id = ?";
        String sqlUpdate = "UPDATE studios SET name=?, address=?, price=?, rating=?, description=? WHERE id=?";
        String sqlInstId = "SELECT id FROM instruments WHERE name = ?";
        String sqlLink = "INSERT INTO studio_instruments(studio_id, instrument_id) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement pstmtDel = conn.prepareStatement(sqlDelLinks);
                pstmtDel.setInt(1, studio.getId());
                pstmtDel.executeUpdate();
                pstmtDel.close();

                PreparedStatement pstmtUpd = conn.prepareStatement(sqlUpdate);
                pstmtUpd.setString(1, studio.getName());
                pstmtUpd.setString(2, studio.getAddress());
                pstmtUpd.setDouble(3, studio.getPricePerHour());
                pstmtUpd.setDouble(4, studio.getRating());
                pstmtUpd.setString(5, studio.getDescription());
                pstmtUpd.setInt(6, studio.getId());
                pstmtUpd.executeUpdate();
                pstmtUpd.close();

                PreparedStatement pstmtInst = conn.prepareStatement(sqlInstId);
                PreparedStatement pstmtLink = conn.prepareStatement(sqlLink);

                for (Instrument inst : studio.getInstruments()) {
                    pstmtInst.setString(1, inst.getName());
                    ResultSet rsInst = pstmtInst.executeQuery();
                    if (rsInst.next()) {
                        pstmtLink.setInt(1, studio.getId());
                        pstmtLink.setInt(2, rsInst.getInt("id"));
                        pstmtLink.executeUpdate();
                    }
                    rsInst.close();
                }
                pstmtInst.close(); pstmtLink.close();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка обновления: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM studios WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка удаления: " + e.getMessage());
        }
    }

    @Override
    public List<Studio> findByFilter(SearchFilter filter) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, GROUP_CONCAT(i.name) as inst_names FROM studios s " +
                        "LEFT JOIN studio_instruments si ON s.id = si.studio_id " +
                        "LEFT JOIN instruments i ON si.instrument_id = i.id "
        );

        List<Object> params = new ArrayList<>();
        boolean hasWhere = false;

        if (!filter.getRequiredInstruments().isEmpty()) {
            sql.append("WHERE s.id IN (SELECT studio_id FROM studio_instruments si2 JOIN instruments i2 ON si2.instrument_id = i2.id WHERE i2.name IN (");
            Object[] instruments = filter.getRequiredInstruments().toArray();
            for (int j = 0; j < instruments.length; j++) {
                sql.append("?");
                params.add(instruments[j]);
                if (j < instruments.length - 1) sql.append(", ");
            }
            sql.append(") GROUP BY studio_id HAVING COUNT(DISTINCT i2.name) = ").append(filter.getRequiredInstruments().size()).append(")");
            hasWhere = true;
        }

        if (filter.getMinPrice() > 0 || filter.getMaxPrice() < Double.MAX_VALUE) {
            sql.append(hasWhere ? " AND" : " WHERE").append(" s.price >= ? AND s.price <= ?");
            params.add(filter.getMinPrice());
            params.add(filter.getMaxPrice());
            hasWhere = true;
        }

        if (filter.getMinRating() > 0) {
            sql.append(hasWhere ? " AND" : " WHERE").append(" s.rating >= ?");
            params.add(filter.getMinRating());
        }

        sql.append(" GROUP BY s.id");

        List<Studio> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                if (params.get(i) instanceof Double) pstmt.setDouble(i + 1, (Double) params.get(i));
                else pstmt.setString(i + 1, (String) params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapRowToStudio(rs));
        } catch (SQLException e) {
            System.err.println("Ошибка поиска: " + e.getMessage());
        }
        return list;
    }

    private Studio mapRowToStudio(ResultSet rs) throws SQLException {
        List<Instrument> instruments = new ArrayList<>();
        String instNames = rs.getString("inst_names");
        if (instNames != null && !instNames.isEmpty()) {
            for (String name : instNames.split(",")) {
                instruments.add(new Instrument(name.trim()));
            }
        }

        return new Studio(
                rs.getInt("id"), rs.getString("name"), rs.getString("address"),
                rs.getDouble("price"), rs.getDouble("rating"),
                instruments, rs.getString("description")
        );
    }
}