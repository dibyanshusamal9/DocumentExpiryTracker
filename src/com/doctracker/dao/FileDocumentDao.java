package com.doctracker.dao;

import com.doctracker.model.Document;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Offline fallback used when MySQL is not reachable. Keeps documents in a single serialized file
 * (data/documents.dat), written atomically after every change.
 */
public class FileDocumentDao implements DocumentDao {

    private final Path file;
    private final List<Document> docs = new ArrayList<>();
    private int nextId = 1;

    public FileDocumentDao(Path file) throws DaoException {
        this.file = file;
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() throws DaoException {
        if (!Files.exists(file)) return;
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            docs.addAll((List<Document>) in.readObject());
            nextId = docs.stream().mapToInt(Document::getId).max().orElse(0) + 1;
        } catch (IOException | ClassNotFoundException e) {
            throw new DaoException("Could not read " + file + ": " + e.getMessage(), e);
        }
    }

    private void save() throws DaoException {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeObject(new ArrayList<>(docs));
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DaoException("Could not write " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized List<Document> findAll() {
        List<Document> copy = new ArrayList<>(docs);
        copy.sort(Comparator.comparing(Document::getExpiryDate));
        return copy;
    }

    @Override
    public synchronized Document findById(int id) {
        return docs.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
    }

    @Override
    public synchronized Document insert(Document d) throws DaoException {
        d.setId(nextId++);
        docs.add(d);
        save();
        return d;
    }

    @Override
    public synchronized void update(Document d) throws DaoException {
        for (int i = 0; i < docs.size(); i++) {
            if (docs.get(i).getId() == d.getId()) {
                docs.set(i, d);
                save();
                return;
            }
        }
        throw new DaoException("Document " + d.getId() + " not found");
    }

    @Override
    public synchronized void delete(int id) throws DaoException {
        if (docs.removeIf(d -> d.getId() == id)) save();
    }

    @Override
    public String describe() {
        return "Offline file: " + file.toAbsolutePath();
    }
}
