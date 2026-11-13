package com.doctracker;

import com.doctracker.dao.FileDocumentDao;
import com.doctracker.model.Document;
import com.doctracker.model.DocumentType;
import com.doctracker.model.ExpiryStatus;
import com.doctracker.service.ReminderService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Dependency-free checks for the reminder logic and the offline store. Run: test.bat */
public class SelfTest {
    private static int passed, failed;

    public static void main(String[] args) throws Exception {
        LocalDate today = LocalDate.of(2026, 11, 13);

        Document passport = doc("Passport", DocumentType.PASSPORT, today.plusDays(100), 180);
        Document licence = doc("Licence", DocumentType.DRIVING_LICENSE, today.plusDays(45), 30);
        Document insurance = doc("Insurance", DocumentType.INSURANCE, today.minusDays(3), 15);
        Document warranty = doc("Warranty", DocumentType.WARRANTY, today, 30);

        check("passport inside 180-day window is DUE_SOON", passport.status(today) == ExpiryStatus.DUE_SOON);
        check("licence 45 days out with 30-day window is VALID", licence.status(today) == ExpiryStatus.VALID);
        check("insurance past expiry is EXPIRED", insurance.status(today) == ExpiryStatus.EXPIRED);
        check("expiring today counts as DUE_SOON, not expired", warranty.status(today) == ExpiryStatus.DUE_SOON);
        check("daysLeft negative when expired", insurance.daysLeft(today) == -3);

        ReminderService rs = new ReminderService();
        List<Document> due = rs.needingAttention(List.of(passport, licence, insurance, warranty), today);
        check("3 documents need attention", due.size() == 3);
        check("most urgent (expired) first", due.get(0) == insurance);
        check("counts: 1 expired", rs.counts(List.of(passport, licence, insurance, warranty), today).get(ExpiryStatus.EXPIRED) == 1);
        check("describe expired", ReminderService.describe(insurance, today).equals("Insurance expired 3 days ago"));
        check("describe today", ReminderService.describe(warranty, today).equals("Warranty expires TODAY"));

        Document withIssue = doc("Policy", DocumentType.INSURANCE, today.plusDays(10), 15);
        withIssue.setIssueDate(today.plusDays(10).minusYears(1));
        check("renewal keeps previous validity length",
                ReminderService.suggestRenewal(withIssue, today).equals(today.plusDays(10).plusDays(365)));
        check("renewal of expired doc starts from today",
                ReminderService.suggestRenewal(insurance, today).equals(today.plusYears(1)));

        // Offline store round trip
        Path dir = Files.createTempDirectory("doctracker-test");
        Path f = dir.resolve("documents.dat");
        FileDocumentDao dao = new FileDocumentDao(f);
        dao.insert(passport);
        dao.insert(insurance);
        insurance.setNotes("renewed online");
        dao.update(insurance);
        FileDocumentDao reopened = new FileDocumentDao(f);
        check("file store persists 2 docs", reopened.findAll().size() == 2);
        check("file store sorts by expiry", reopened.findAll().get(0).getTitle().equals("Insurance"));
        check("file store keeps updates", "renewed online".equals(reopened.findById(insurance.getId()).getNotes()));
        reopened.delete(passport.getId());
        check("file store delete", new FileDocumentDao(f).findAll().size() == 1);
        Document third = reopened.insert(doc("New", DocumentType.OTHER, today, 1));
        check("ids keep increasing after reload", third.getId() == 3);

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static Document doc(String title, DocumentType t, LocalDate expiry, int remind) {
        Document d = new Document();
        d.setTitle(title);
        d.setType(t);
        d.setExpiryDate(expiry);
        d.setReminderDays(remind);
        return d;
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
        if (ok) passed++; else failed++;
    }
}
