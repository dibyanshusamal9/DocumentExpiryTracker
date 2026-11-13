package com.doctracker.dao;

import com.doctracker.model.Document;

import java.util.List;

/** Storage contract. MySQL is the main implementation; a local file store is the offline fallback. */
public interface DocumentDao {
    List<Document> findAll() throws DaoException;
    Document findById(int id) throws DaoException;
    Document insert(Document d) throws DaoException;
    void update(Document d) throws DaoException;
    void delete(int id) throws DaoException;
    /** Short name shown in the status bar, e.g. "MySQL @ localhost". */
    String describe();
}
