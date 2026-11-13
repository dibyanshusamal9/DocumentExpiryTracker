"""Generates docs/Project_Report.pdf for the Personal Document Expiry Tracker."""
import sys
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.graphics.shapes import Drawing, Rect, String, Line, Polygon
from reportlab.platypus import (BaseDocTemplate, Frame, PageTemplate, Paragraph, Spacer, PageBreak,
                                Table, TableStyle, Image, KeepTogether, NextPageTemplate, CondPageBreak)
from reportlab.platypus.tableofcontents import TableOfContents

PROJECT = Path(sys.argv[1])
OUT = PROJECT / "docs" / "Project_Report.pdf"
SHOTS = PROJECT / "docs" / "screenshots"

NAVY = colors.HexColor("#1F3A5F")
ACCENT = colors.HexColor("#2F5D8A")
LIGHT = colors.HexColor("#EEF2F7")
RED = colors.HexColor("#F8D7DA")
AMBER = colors.HexColor("#FFF3CD")
GREEN = colors.HexColor("#D4EDDA")
GRID = colors.HexColor("#B8C2CF")

ss = getSampleStyleSheet()
body = ParagraphStyle("body", parent=ss["Normal"], fontName="Times-Roman", fontSize=11.5, leading=16.5,
                      alignment=TA_JUSTIFY, spaceAfter=7)
bullet = ParagraphStyle("bullet", parent=body, leftIndent=16, bulletIndent=4, spaceAfter=3)
h1 = ParagraphStyle("h1", parent=ss["Heading1"], fontName="Helvetica-Bold", fontSize=17, textColor=NAVY,
                    spaceBefore=4, spaceAfter=12, keepWithNext=1)
h2 = ParagraphStyle("h2", parent=ss["Heading2"], fontName="Helvetica-Bold", fontSize=13, textColor=ACCENT,
                    spaceBefore=10, spaceAfter=6, keepWithNext=1)
caption = ParagraphStyle("caption", parent=body, fontName="Times-Italic", fontSize=10, alignment=TA_CENTER,
                         textColor=colors.HexColor("#555555"), spaceBefore=4, spaceAfter=12, keepWithNext=1)
cell = ParagraphStyle("cell", parent=ss["Normal"], fontName="Helvetica", fontSize=9.2, leading=12)
cellb = ParagraphStyle("cellb", parent=cell, fontName="Helvetica-Bold", textColor=colors.white)
code = ParagraphStyle("code", parent=ss["Code"], fontName="Courier", fontSize=8.4, leading=11)


def Preformatted(text, style):
    from reportlab.platypus import Preformatted as _Pre
    t = Table([[_Pre(text, style)]], colWidths=[16.4 * cm])
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F5F7FA")),
                           ("BOX", (0, 0), (-1, -1), 0.5, GRID), ("LEFTPADDING", (0, 0), (-1, -1), 8),
                           ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6)]))
    return KeepTogether([t, Spacer(1, 10)])


class ReportDoc(BaseDocTemplate):
    def __init__(self, fn):
        super().__init__(fn, pagesize=A4, leftMargin=2.4 * cm, rightMargin=2.2 * cm, topMargin=2.2 * cm,
                         bottomMargin=2.2 * cm, title="Personal Document Expiry Tracker - Project Report",
                         author="Dibyanshu Samal", subject="Project Report")
        fr = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="f")
        self.addPageTemplates([PageTemplate("cover", [fr], onPage=lambda c, d: None),
                               PageTemplate("body", [fr], onPage=self.decorate)])

    def decorate(self, c, d):
        w, h = A4
        c.saveState()
        c.setStrokeColor(GRID)
        c.setLineWidth(0.6)
        c.line(self.leftMargin, h - 1.5 * cm, w - self.rightMargin, h - 1.5 * cm)
        c.setFont("Helvetica", 8.5)
        c.setFillColor(colors.HexColor("#667788"))
        c.drawString(self.leftMargin, h - 1.3 * cm, "Personal Document Expiry Tracker")
        c.drawRightString(w - self.rightMargin, h - 1.3 * cm, "Project Report")
        c.line(self.leftMargin, 1.6 * cm, w - self.rightMargin, 1.6 * cm)
        c.drawCentredString(w / 2, 1.1 * cm, str(c.getPageNumber()))
        c.restoreState()

    def afterFlowable(self, f):
        if isinstance(f, Paragraph) and f.style.name in ("h1", "h2"):
            level = 0 if f.style.name == "h1" else 1
            key = "sec%d" % id(f)
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(f.getPlainText(), key, level=level, closed=False)
            self.notify("TOCEntry", (level, f.getPlainText(), self.page, key))


lead = ParagraphStyle("lead", parent=body, keepWithNext=1)
h2s = ParagraphStyle("h2", parent=h2, keepWithNext=0)


def diagram(drawing, text, intro=None, heading=None):
    parts = ([P(heading, h2)] if heading else []) + ([P(intro)] if intro else []) + [centered(drawing), P(text, caption)]
    return KeepTogether(parts)


def P(t, s=body):
    return Paragraph(t, s)


def bullets(items):
    return [Paragraph(i, bullet, bulletText="•") for i in items]


def table(rows, widths, header=True, zebra=True, extra=None):
    data = [[Paragraph(str(c), cellb if (header and r == 0) else cell) for c in row] for r, row in enumerate(rows)]
    t = Table(data, colWidths=widths, repeatRows=1 if header else 0)
    st = [("GRID", (0, 0), (-1, -1), 0.5, GRID), ("VALIGN", (0, 0), (-1, -1), "TOP"),
          ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
          ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5)]
    if header:
        st.append(("BACKGROUND", (0, 0), (-1, 0), ACCENT))
    if zebra:
        for r in range(1 if header else 0, len(rows)):
            if r % 2 == 0:
                st.append(("BACKGROUND", (0, r), (-1, r), LIGHT))
    if extra:
        st += extra
    t.setStyle(TableStyle(st))
    return t


def figure(path, width, text):
    from reportlab.lib.utils import ImageReader
    iw, ih = ImageReader(str(path)).getSize()
    img = Image(str(path), width=width, height=width * ih / iw)
    img.hAlign = "CENTER"
    framed = Table([[img]], style=[("BOX", (0, 0), (-1, -1), 0.6, GRID), ("LEFTPADDING", (0, 0), (-1, -1), 0),
                                   ("RIGHTPADDING", (0, 0), (-1, -1), 0), ("TOPPADDING", (0, 0), (-1, -1), 0),
                                   ("BOTTOMPADDING", (0, 0), (-1, -1), 0)])
    return KeepTogether([framed, P(text, caption)])


# ---------------------------------------------------------------- diagrams

def box(d, x, y, w, h, title, sub=None, fill=LIGHT, stroke=ACCENT, title_size=9.5):
    d.add(Rect(x, y, w, h, rx=5, ry=5, fillColor=fill, strokeColor=stroke, strokeWidth=1))
    if sub:
        d.add(String(x + w / 2, y + h / 2 + 3, title, fontName="Helvetica-Bold", fontSize=title_size,
                     textAnchor="middle", fillColor=NAVY))
        d.add(String(x + w / 2, y + h / 2 - 9, sub, fontName="Helvetica", fontSize=7.8, textAnchor="middle",
                     fillColor=colors.HexColor("#445566")))
    else:
        d.add(String(x + w / 2, y + h / 2 - 3, title, fontName="Helvetica-Bold", fontSize=title_size,
                     textAnchor="middle", fillColor=NAVY))


def arrow(d, x1, y1, x2, y2, label=None, color=colors.HexColor("#556677")):
    import math
    d.add(Line(x1, y1, x2, y2, strokeColor=color, strokeWidth=1.1))
    a = math.atan2(y2 - y1, x2 - x1)
    s = 6
    d.add(Polygon([x2, y2, x2 - s * math.cos(a - 0.4), y2 - s * math.sin(a - 0.4),
                   x2 - s * math.cos(a + 0.4), y2 - s * math.sin(a + 0.4)], fillColor=color, strokeColor=color))
    if label:
        d.add(String((x1 + x2) / 2 + 4, (y1 + y2) / 2 + 2, label, fontName="Helvetica-Oblique", fontSize=7.5,
                     fillColor=colors.HexColor("#556677")))


def architecture():
    d = Drawing(440, 330)
    layers = [(250, "PRESENTATION LAYER  (Java Swing)"), (165, "BUSINESS LOGIC LAYER"),
              (85, "DATA ACCESS LAYER  (DAO pattern)"), (0, "STORAGE")]
    for y, name in layers:
        d.add(Rect(0, y, 440, 75 if y else 70, fillColor=colors.HexColor("#F7F9FC"),
                   strokeColor=GRID, strokeWidth=0.6, strokeDashArray=[3, 2]))
        d.add(String(6, y + (63 if y else 58), name, fontName="Helvetica-Bold", fontSize=7.5, fillColor=ACCENT))
    box(d, 20, 260, 120, 40, "MainFrame", "dashboard, table, filters")
    box(d, 160, 260, 120, 40, "DocumentDialog", "add / edit form")
    box(d, 300, 260, 120, 40, "System Tray", "desktop notifications")
    box(d, 120, 172, 200, 40, "ReminderService", "status, reminders, renewal rules")
    box(d, 120, 92, 200, 38, "DocumentDao  (interface)", "findAll / insert / update / delete")
    box(d, 20, 6, 150, 46, "MySqlDocumentDao", "JDBC + PreparedStatement", fill=GREEN)
    box(d, 270, 6, 150, 46, "FileDocumentDao", "offline: documents.dat", fill=AMBER)
    arrow(d, 80, 260, 160, 212)
    arrow(d, 220, 260, 220, 212)
    arrow(d, 360, 260, 280, 212)
    arrow(d, 220, 172, 220, 130)
    arrow(d, 180, 92, 110, 52, "implements")
    arrow(d, 260, 92, 330, 52, "implements")
    return d


def er_diagram():
    d = Drawing(440, 250)
    cols = [("id", "INT", "PK, AUTO_INCREMENT"), ("title", "VARCHAR(120)", "NOT NULL"),
            ("doc_type", "VARCHAR(30)", "NOT NULL"), ("doc_number", "VARCHAR(60)", ""),
            ("issuer", "VARCHAR(120)", ""), ("issue_date", "DATE", ""),
            ("expiry_date", "DATE", "NOT NULL, INDEX"), ("reminder_days", "INT", "DEFAULT 30"),
            ("notes", "VARCHAR(500)", ""), ("created_at", "TIMESTAMP", "DEFAULT NOW"),
            ("updated_at", "TIMESTAMP", "ON UPDATE NOW")]
    x, w, rh = 70, 300, 19
    top = 240
    d.add(Rect(x, top - 24, w, 24, fillColor=NAVY, strokeColor=NAVY))
    d.add(String(x + w / 2, top - 16, "documents", fontName="Helvetica-Bold", fontSize=11,
                 textAnchor="middle", fillColor=colors.white))
    for i, (n, t, c) in enumerate(cols):
        y = top - 24 - (i + 1) * rh
        d.add(Rect(x, y, w, rh, fillColor=LIGHT if i % 2 else colors.white, strokeColor=GRID, strokeWidth=0.5))
        font = "Helvetica-Bold" if n in ("id", "expiry_date") else "Helvetica"
        d.add(String(x + 8, y + 6, n, fontName=font, fontSize=8.5, fillColor=NAVY))
        d.add(String(x + 115, y + 6, t, fontName="Courier", fontSize=8.2, fillColor=colors.black))
        d.add(String(x + 205, y + 6, c, fontName="Helvetica-Oblique", fontSize=7.5,
                     fillColor=colors.HexColor("#556677")))
    return d


def flowchart():
    d = Drawing(440, 330)

    def diamond(cx, cy, w, h, text):
        d.add(Polygon([cx, cy + h / 2, cx + w / 2, cy, cx, cy - h / 2, cx - w / 2, cy],
                      fillColor=AMBER, strokeColor=ACCENT, strokeWidth=1))
        d.add(String(cx, cy - 3, text, fontName="Helvetica-Bold", fontSize=8.5, textAnchor="middle", fillColor=NAVY))

    box(d, 150, 295, 140, 28, "Load all documents", fill=colors.white)
    box(d, 130, 240, 180, 32, "days_left = expiry_date - today", fill=colors.white, title_size=8.8)
    diamond(220, 185, 160, 50, "days_left < 0 ?")
    diamond(220, 105, 190, 50, "days_left <= reminder_days ?")
    box(d, 345, 168, 85, 34, "EXPIRED", fill=RED)
    box(d, 345, 88, 85, 34, "RENEW SOON", fill=AMBER)
    box(d, 177, 10, 86, 34, "VALID", fill=GREEN)
    box(d, 337, 10, 90, 40, "Remind user", "pop-up + tray alert", fill=colors.white, title_size=8.8)
    arrow(d, 220, 295, 220, 272)
    arrow(d, 220, 240, 220, 210)
    arrow(d, 300, 185, 345, 185, "yes")
    arrow(d, 220, 160, 220, 130, "no")
    arrow(d, 315, 105, 345, 105, "yes")
    arrow(d, 220, 80, 220, 44, "no")
    g = colors.HexColor("#556677")
    arrow(d, 387, 88, 387, 50)                       # RENEW SOON -> alert
    d.add(Line(430, 185, 438, 185, strokeColor=g, strokeWidth=1.1))   # EXPIRED -> alert (right side)
    d.add(Line(438, 185, 438, 30, strokeColor=g, strokeWidth=1.1))
    arrow(d, 438, 30, 427, 30)
    return d


def class_diagram():
    d = Drawing(440, 262)

    def cls(x, y, w, name, fields, stereo=None):
        lh = 11
        h = 22 + lh * len(fields) + 6
        d.add(Rect(x, y - h, w, h, fillColor=colors.white, strokeColor=ACCENT, strokeWidth=1))
        d.add(Rect(x, y - 22, w, 22, fillColor=LIGHT, strokeColor=ACCENT, strokeWidth=1))
        if stereo:
            d.add(String(x + w / 2, y - 9, stereo, fontName="Helvetica-Oblique", fontSize=6.8,
                         textAnchor="middle", fillColor=colors.HexColor("#556677")))
            d.add(String(x + w / 2, y - 19, name, fontName="Helvetica-Bold", fontSize=8.8,
                         textAnchor="middle", fillColor=NAVY))
        else:
            d.add(String(x + w / 2, y - 15, name, fontName="Helvetica-Bold", fontSize=9,
                         textAnchor="middle", fillColor=NAVY))
        for i, f in enumerate(fields):
            d.add(String(x + 6, y - 22 - lh * (i + 1), f, fontName="Courier", fontSize=7.2, fillColor=colors.black))
        return h

    cls(0, 240, 165, "Document", ["- id : int", "- title : String", "- type : DocumentType",
                                  "- documentNumber : String", "- issuer : String", "- issueDate : LocalDate",
                                  "- expiryDate : LocalDate", "- reminderDays : int", "- notes : String",
                                  "+ daysLeft(today) : long", "+ status(today) : ExpiryStatus"])
    cls(195, 240, 110, "DocumentType", ["PASSPORT", "DRIVING_LICENSE", "INSURANCE", "WARRANTY",
                                        "VEHICLE_RC, ID_CARD", "SUBSCRIPTION, OTHER"], "<<enum>>")
    cls(330, 240, 110, "ExpiryStatus", ["EXPIRED", "DUE_SOON", "VALID"], "<<enum>>")
    cls(195, 125, 245, "ReminderService", ["+ needingAttention(list, today) : List",
                                           "+ counts(list, today) : Map",
                                           "+ describe(doc, today) : String",
                                           "+ suggestRenewal(doc, today) : LocalDate"])
    cls(0, 75, 165, "DocumentDao", ["+ findAll() / findById(id)", "+ insert(d) / update(d)",
                                    "+ delete(id)"], "<<interface>>")
    g = colors.HexColor("#556677")
    arrow(d, 165, 215, 195, 215)                       # Document -> DocumentType
    d.add(Line(82, 240, 82, 254, strokeColor=g, strokeWidth=1.1))   # Document -> ExpiryStatus (over the top)
    d.add(Line(82, 254, 385, 254, strokeColor=g, strokeWidth=1.1))
    arrow(d, 385, 254, 385, 240)
    arrow(d, 195, 95, 165, 120)                        # ReminderService -> Document
    d.add(String(172, 98, "uses", fontName="Helvetica-Oblique", fontSize=7.5, fillColor=g))
    arrow(d, 82, 75, 82, 91)                           # DocumentDao -> Document
    d.add(String(88, 80, "persists", fontName="Helvetica-Oblique", fontSize=7.5, fillColor=g))
    return d


def centered(drawing):
    t = Table([[drawing]], colWidths=[16.4 * cm])
    t.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER")]))
    return t


# ---------------------------------------------------------------- content

def cover():
    s = []
    s.append(Spacer(1, 1.2 * cm))
    s.append(P("PROJECT REPORT", ParagraphStyle("k", fontName="Helvetica-Bold", fontSize=12, textColor=ACCENT,
                                                 alignment=TA_CENTER, spaceAfter=18)))
    s.append(P("Personal Document Expiry Tracker",
               ParagraphStyle("t", fontName="Helvetica-Bold", fontSize=28, leading=34, textColor=NAVY,
                              alignment=TA_CENTER, spaceAfter=10)))
    s.append(P("Passport &middot; Insurance &middot; License &middot; Warranties",
               ParagraphStyle("st", fontName="Helvetica", fontSize=13, textColor=colors.HexColor("#556677"),
                              alignment=TA_CENTER, spaceAfter=30)))
    d = Drawing(160, 150)
    d.add(Rect(35, 10, 80, 120, rx=8, ry=8, fillColor=ACCENT, strokeColor=ACCENT))
    for i in range(4):
        d.add(Line(52, 105 - i * 20, 98, 105 - i * 20, strokeColor=colors.white, strokeWidth=4))
    from reportlab.graphics.shapes import Circle
    d.add(Circle(112, 28, 30, fillColor=colors.HexColor("#E0A100"), strokeColor=colors.white, strokeWidth=3))
    d.add(Line(112, 28, 112, 46, strokeColor=colors.white, strokeWidth=4))
    d.add(Line(112, 28, 126, 28, strokeColor=colors.white, strokeWidth=4))
    s.append(centered(d))
    s.append(Spacer(1, 1.2 * cm))
    s.append(P("A Java desktop application with MySQL database for automatic tracking of document "
               "expiry dates and timely renewal reminders",
               ParagraphStyle("d", fontName="Times-Italic", fontSize=12.5, leading=17, alignment=TA_CENTER,
                              spaceAfter=40)))
    info = [["Submitted by", "Dibyanshu Samal"], ["Roll No.", "____________________"],
            ["Course / Subject", "____________________"], ["Submitted to", "____________________"],
            ["Technology", "Java (Swing, JDBC), MySQL 8"], ["Date", "November 2026"]]
    t = Table([[P(a, ParagraphStyle("a", fontName="Helvetica-Bold", fontSize=10.5, textColor=NAVY)),
                P(b, ParagraphStyle("b", fontName="Helvetica", fontSize=10.5))] for a, b in info],
              colWidths=[4.5 * cm, 7.5 * cm])
    t.setStyle(TableStyle([("LINEBELOW", (0, 0), (-1, -1), 0.4, GRID), ("TOPPADDING", (0, 0), (-1, -1), 6),
                           ("BOTTOMPADDING", (0, 0), (-1, -1), 6)]))
    s.append(t)
    return s


def content():
    s = []
    # Abstract
    s.append(P("Abstract", h1))
    s.append(P("People own many documents that are valid only for a limited time: passports, driving licences, "
               "vehicle registration and pollution certificates, insurance policies and product warranties. "
               "Their expiry dates are spread across the year and are easy to forget. A lapsed insurance policy "
               "can leave a person uncovered, an expired licence attracts a fine, a passport close to expiry can "
               "cancel travel plans, and a missed warranty date turns a free repair into a paid one."))
    s.append(P("The <b>Personal Document Expiry Tracker</b> solves this with a single desktop application in which "
               "the user records every important document once. The system computes how many days remain for each "
               "document, classifies it as <i>Expired</i>, <i>Renew soon</i> or <i>Valid</i> using a reminder window "
               "that can be set per document, and alerts the user through a start-up summary and Windows desktop "
               "notifications. It is built with Java Swing for the user interface, JDBC for data access and MySQL for "
               "storage, following a layered architecture with the DAO design pattern. An offline file-based store "
               "keeps the application usable when the database server is unavailable."))
    s.append(P("<b>Keywords:</b> document management, expiry tracking, reminders, Java Swing, JDBC, MySQL, DAO pattern."))
    s.append(PageBreak())

    toc = TableOfContents()
    toc.levelStyles = [ParagraphStyle("t0", fontName="Helvetica-Bold", fontSize=10.5, leading=16, leftIndent=0),
                       ParagraphStyle("t1", fontName="Helvetica", fontSize=9.5, leading=13, leftIndent=18)]
    s.append(P("Table of Contents", ParagraphStyle("tc", parent=h1)))
    s.append(toc)
    s.append(PageBreak())

    # 1 Introduction
    s.append(P("1. Introduction", h1))
    s.append(P("1.1 Background", h2))
    s.append(P("An average household keeps track of a dozen or more documents with fixed validity. Each comes from a "
               "different authority, has a different renewal cycle (a pollution certificate every six months, an "
               "insurance policy every year, a passport every ten years), and needs action at a different lead time: "
               "a passport should be renewed months before travel, while an insurance policy only needs a reminder "
               "a couple of weeks ahead. Most people rely on memory, scattered calendar entries or the issuer's own "
               "reminders, which are unreliable or absent."))
    s.append(P("1.2 Purpose of the Project", h2))
    s.append(P("The project provides a simple, centralized place to record these documents and have the computer do the "
               "remembering. Once a document is entered, the user does not need to think about it again until the "
               "application says it is time to renew."))
    s.append(P("1.3 Scope", h2))
    s.extend(bullets([
        "Single-user desktop application for Windows (runs on any OS with Java 17+).",
        "Tracks any document with an expiry date: passport, driving licence, insurance, warranty, vehicle RC/PUC, "
        "ID cards, subscriptions and others.",
        "Stores data in a MySQL database; falls back to a local file when MySQL is not reachable.",
        "Reminds the user in-app and through desktop notifications. Email/SMS reminders are listed as future scope.",
    ]))

    # 2 Problem & objectives
    s.append(P("2. Problem Statement and Objectives", h1))
    s.append(P("2.1 Problem Statement", h2))
    s.append(P("People often forget the expiry dates of important documents such as passports, driving licences, "
               "insurance policies and warranties. Missing these dates causes penalties, inconvenience or financial "
               "loss. There is a need for a simple system that tracks all such documents in one place and reminds "
               "the user automatically before each one expires."))
    s.append(P("2.2 Objectives", h2))
    s.extend(bullets([
        "Maintain a central record of personal documents with their number, issuer, issue date and expiry date.",
        "Automatically compute the remaining validity of every document and classify its status.",
        "Allow a different reminder lead time for each document, with sensible defaults per document type.",
        "Notify the user of expired and soon-to-expire documents without the user having to search for them.",
        "Make renewal quick to record, and let the user search, filter and export the data.",
        "Keep the design simple, modular and built on freely available technologies (Java and MySQL).",
    ]))
    s.append(P("2.3 Existing System and its Limitations", h2))
    s.append(table([
        ["Current method", "Limitation"],
        ["Memory / physical file folder", "Unreliable; no alert; documents scattered."],
        ["Phone calendar entries", "Entered one by one; no overview of all documents; no status at a glance."],
        ["Issuer reminders (SMS/email)", "Not every issuer sends them; arrive too late for long processes like passports."],
        ["Spreadsheet", "Requires manual checking and formula upkeep; no notification."],
    ], [5.5 * cm, 10.9 * cm]))
    s.append(Spacer(1, 6))
    s.append(P("The proposed system removes these limitations by combining storage, automatic status calculation "
               "and active reminders in one application."))

    # 3 Feasibility
    s.append(P("3. Feasibility Study", h1))
    s.append(P("3.1 Technical Feasibility", h2))
    s.append(P("The system uses mature, well-documented technologies. Java provides the Swing toolkit for the desktop "
               "interface, the <font face='Courier'>java.time</font> API for date arithmetic, and the "
               "<font face='Courier'>SystemTray</font> API for native notifications. JDBC with the MySQL Connector/J "
               "driver provides database access. No special hardware is required. The project is therefore "
               "technically feasible."))
    s.append(P("3.2 Economic Feasibility", h2))
    s.append(P("All tools used are free: the Java Development Kit, MySQL Community Server, MySQL Connector/J and any "
               "standard IDE. The application runs on an ordinary personal computer. Development cost is limited "
               "to the developer's time, and there are no licensing or hosting costs. Against this, the "
               "benefit is avoiding fines, lapsed insurance cover and lost warranty claims, so the project is "
               "economically feasible."))
    s.append(P("3.3 Operational Feasibility", h2))
    s.append(P("The interface uses familiar desktop controls: a table, buttons, a form and colour coding (red, amber, "
               "green) that any user understands without training. Reminders appear automatically, so the system "
               "works even for users who rarely open it. The project is operationally feasible."))

    # 4 Requirements
    s.append(P("4. Requirement Analysis", h1))
    s.append(P("4.1 Functional Requirements", h2))
    s.append(table([
        ["ID", "Requirement"],
        ["FR1", "The user can add a document with title, type, number, issuer, issue date, expiry date, reminder days and notes."],
        ["FR2", "The user can view all documents in a table sorted by urgency (fewest days left first)."],
        ["FR3", "The user can edit and delete documents (deletion asks for confirmation)."],
        ["FR4", "The system computes days left and assigns the status Expired, Renew soon or Valid."],
        ["FR5", "The system shows a reminder summary at start-up and desktop notifications (re-checked hourly, once per document per day)."],
        ["FR6", "The user can mark a document as renewed; the system suggests the next expiry date."],
        ["FR7", "The user can search by text and filter by document type and status."],
        ["FR8", "The dashboard shows total, expired, renew-soon and valid counts."],
        ["FR9", "The user can export all documents to a CSV file."],
        ["FR10", "If MySQL is unavailable, the user can continue with offline file storage."],
    ], [1.4 * cm, 15 * cm]))
    s.append(P("4.2 Non-Functional Requirements", h2))
    s.extend(bullets([
        "<b>Usability:</b> one-window interface, colour-coded status, keyboard shortcuts (Enter to save, Esc to cancel, Delete key).",
        "<b>Reliability:</b> input validation on every form; offline store writes atomically (temporary file then rename).",
        "<b>Security:</b> all SQL uses PreparedStatements, preventing SQL injection; database credentials kept in a separate configuration file.",
        "<b>Performance:</b> indexed expiry_date column; all operations complete instantly for personal-scale data.",
        "<b>Maintainability:</b> layered design (UI, service, DAO) so storage or UI can change independently.",
        "<b>Portability:</b> pure Java; runs on Windows, Linux and macOS with Java 17 or later.",
    ]))
    s.append(P("4.3 Hardware and Software Requirements", h2))
    s.append(table([
        ["Component", "Requirement"],
        ["Processor / RAM", "Any modern dual-core CPU, 4 GB RAM"],
        ["Operating system", "Windows 10/11 (also Linux, macOS)"],
        ["Language / runtime", "Java SE 17 or later (developed and tested on JDK 26)"],
        ["Database", "MySQL Community Server 8.x"],
        ["Driver", "MySQL Connector/J (JDBC)"],
        ["IDE (optional)", "IntelliJ IDEA / Eclipse / VS Code"],
    ], [5 * cm, 11.4 * cm]))

    # 5 Design
    s.append(PageBreak())
    s.append(P("5. System Design", h1))
    s.append(P("5.1 System Architecture", h2))
    s.append(P("The application follows a three-layer architecture. The presentation layer contains the Swing windows and "
               "the system-tray integration. The business logic layer (ReminderService) contains the rules for status "
               "classification, reminder selection and renewal suggestions; it has no UI or database code, which makes "
               "it easy to test. The data access layer hides storage behind the <font face='Courier'>DocumentDao</font> "
               "interface, with a MySQL implementation and an offline file implementation."))
    s.append(diagram(architecture(), "Figure 5.1: Layered architecture of the system"))
    s.append(diagram(class_diagram(), "Figure 5.2: Main classes and their relationships", heading="5.2 Class Design", intro=
                     "The core domain class is <font face='Courier'>Document</font>. Two enumerations describe the document "
                     "category (with a default reminder window for each) and the computed status (with its display colour)."))
    s.append(P("5.3 Database Design", h2))
    s.append(P("A single table is sufficient for a personal tracker. The document type is stored as text (the enum name), "
               "so new types can be added without a schema change. <font face='Courier'>expiry_date</font> is indexed "
               "because every view sorts or filters by it. Status is <i>not</i> stored: it is derived from today's "
               "date each time, so it can never become stale."))
    s.append(diagram(er_diagram(), "Figure 5.3: Schema of the documents table"))
    s.append(table([
        ["Column", "Description"],
        ["title", "User-friendly name, e.g. \"My Passport\""],
        ["doc_type", "PASSPORT, DRIVING_LICENSE, INSURANCE, WARRANTY, VEHICLE_RC, ID_CARD, SUBSCRIPTION, OTHER"],
        ["doc_number / issuer", "Passport or policy number; issuing authority, insurer or seller"],
        ["issue_date", "Optional; used to suggest the next expiry with the same validity length"],
        ["expiry_date", "Date the document stops being valid (required)"],
        ["reminder_days", "How many days before expiry the document enters \"Renew soon\""],
    ], [4.2 * cm, 12.2 * cm]))
    s.append(P("5.4 Status and Reminder Logic", h2))
    s.append(P("For each document the system calculates <i>days_left = expiry_date - today</i>. A negative value means "
               "the document has expired. If the value is within the document's reminder window, it is due for "
               "renewal. Otherwise it is valid. Documents that are expired or due are collected, sorted by urgency "
               "and shown to the user."))
    s.append(diagram(flowchart(), "Figure 5.4: Flowchart of status classification and reminders"))
    s.append(KeepTogether(table([
        ["Document type", "Default reminder", "Reason"],
        ["Passport", "180 days", "Many countries require 6 months' validity; renewal takes weeks"],
        ["Driving licence", "30 days", "Renewal at RTO needs an appointment"],
        ["Insurance policy", "15 days", "Renew before lapse to keep no-claim bonus"],
        ["Warranty", "30 days", "Time to get pending issues repaired under warranty"],
        ["Vehicle RC / PUC", "15 days", "Short validity; fines for an expired certificate"],
        ["Subscription", "7 days", "Decide to renew or cancel before auto-debit"],
    ], [4 * cm, 3.2 * cm, 9.2 * cm])))

    # 6 Implementation
    s.append(PageBreak())
    s.append(P("6. Implementation", h1))
    s.append(P("6.1 Project Structure", h2))
    s.append(Preformatted(
        "DocumentExpiryTracker/\n"
        "  src/com/doctracker/\n"
        "    Main.java                    entry point; chooses MySQL or offline storage\n"
        "    model/Document.java          domain object, daysLeft() and status()\n"
        "    model/DocumentType.java      enum with default reminder days\n"
        "    model/ExpiryStatus.java      EXPIRED / DUE_SOON / VALID with colours\n"
        "    dao/DocumentDao.java         storage interface\n"
        "    dao/MySqlDocumentDao.java    JDBC implementation\n"
        "    dao/FileDocumentDao.java     offline implementation\n"
        "    service/ReminderService.java reminder and renewal rules\n"
        "    ui/MainFrame.java            main window, dashboard, tray\n"
        "    ui/DocumentDialog.java       add / edit form\n"
        "  test/com/doctracker/SelfTest.java\n"
        "  sql/schema.sql   db.properties   build.bat   run.bat   test.bat", code))
    s.append(P("6.2 Modules", h2))
    s.append(table([
        ["Module", "Responsibility"],
        ["Document management", "Add, edit, delete and list documents through DocumentDialog and the main table."],
        ["Status engine", "Document.daysLeft() and Document.status() compute validity from today's date."],
        ["Reminder module", "ReminderService selects documents needing attention; MainFrame shows the start-up summary "
                            "and a javax.swing.Timer re-checks every hour and raises tray notifications."],
        ["Renewal module", "\"Mark renewed\" suggests a new expiry date that keeps the previous validity length."],
        ["Search and filter", "TableRowSorter with a custom RowFilter combines text search, type and status filters."],
        ["Data access", "DocumentDao with MySQL (JDBC) and offline (serialized file) implementations."],
        ["Export", "Writes all documents to a CSV file that opens in Excel."],
    ], [4 * cm, 12.4 * cm]))
    s.append(P("6.3 Key Code", h2))
    s.append(CondPageBreak(6 * cm))
    s.append(P("Status calculation in <font face='Courier'>Document.java</font>:", lead))
    s.append(Preformatted(
        "public long daysLeft(LocalDate today) {\n"
        "    return ChronoUnit.DAYS.between(today, expiryDate);\n"
        "}\n\n"
        "public ExpiryStatus status(LocalDate today) {\n"
        "    long d = daysLeft(today);\n"
        "    if (d < 0) return ExpiryStatus.EXPIRED;\n"
        "    if (d <= reminderDays) return ExpiryStatus.DUE_SOON;\n"
        "    return ExpiryStatus.VALID;\n"
        "}", code))
    s.append(CondPageBreak(6 * cm))
    s.append(P("Inserting a document with a PreparedStatement in <font face='Courier'>MySqlDocumentDao.java</font>:", lead))
    s.append(Preformatted(
        "String sql = \"INSERT INTO documents (title, doc_type, doc_number, issuer,\"\n"
        "           + \" issue_date, expiry_date, reminder_days, notes)\"\n"
        "           + \" VALUES (?,?,?,?,?,?,?,?)\";\n"
        "try (Connection c = connect();\n"
        "     PreparedStatement ps = c.prepareStatement(sql,\n"
        "             Statement.RETURN_GENERATED_KEYS)) {\n"
        "    bind(ps, d);                 // sets the 8 parameters safely\n"
        "    ps.executeUpdate();\n"
        "    try (ResultSet keys = ps.getGeneratedKeys()) {\n"
        "        if (keys.next()) d.setId(keys.getInt(1));\n"
        "    }\n"
        "}", code))
    s.append(CondPageBreak(6 * cm))
    s.append(P("Selecting documents that need attention in <font face='Courier'>ReminderService.java</font>:", lead))
    s.append(Preformatted(
        "public List<Document> needingAttention(List<Document> all, LocalDate today) {\n"
        "    return all.stream()\n"
        "            .filter(d -> d.status(today) != ExpiryStatus.VALID)\n"
        "            .sorted(Comparator.comparingLong(d -> d.daysLeft(today)))\n"
        "            .collect(Collectors.toList());\n"
        "}", code))
    s.append(P("6.4 Database Connection", h2))
    s.append(P("Connection settings are read from <font face='Courier'>db.properties</font>. The JDBC URL includes "
               "<font face='Courier'>createDatabaseIfNotExist=true</font>, and the DAO runs "
               "<font face='Courier'>CREATE TABLE IF NOT EXISTS</font> on start-up, so the first run needs no manual "
               "database setup. If the connection fails, the user is told why and offered offline storage."))
    s.append(Preformatted(
        "db.url=jdbc:mysql://localhost:3306/doc_tracker\\\n       ?createDatabaseIfNotExist=true&serverTimezone=UTC\n"
        "db.user=root\n"
        "db.password=", code))

    # 7 Screenshots
    s.append(PageBreak())
    s.append(P("7. Output Screens", h1))
    s.append(figure(SHOTS / "main.png", 16.2 * cm,
                    "Figure 7.1: Main window - dashboard counts and colour-coded documents sorted by urgency"))
    s.append(figure(SHOTS / "filtered.png", 16.2 * cm,
                    "Figure 7.2: Filtering by status \"Renew soon\" (also available by clicking the dashboard card)"))
    s.append(figure(SHOTS / "dialog.png", 8.5 * cm,
                    "Figure 7.3: Add / Edit document form with optional issue date and reminder window"))

    # 8 Testing
    s.append(P("8. Testing", h1))
    s.append(P("The business logic and the offline store were tested with an automated self-test "
               "(<font face='Courier'>test.bat</font>) that uses a fixed reference date (13 Nov 2026) so the "
               "results do not change from day to day. The user interface was tested manually."))
    s.append(P("8.1 Automated Test Cases", h2s))
    tc = [
        ["#", "Test case", "Input", "Expected", "Result"],
        ["1", "Inside reminder window", "Passport, 100 days left, window 180", "Renew soon", "Pass"],
        ["2", "Outside reminder window", "Licence, 45 days left, window 30", "Valid", "Pass"],
        ["3", "Past expiry", "Insurance, expired 3 days ago", "Expired", "Pass"],
        ["4", "Expires today (boundary)", "Warranty, 0 days left", "Renew soon", "Pass"],
        ["5", "Days left when expired", "Expired 3 days ago", "-3", "Pass"],
        ["6", "Documents needing attention", "4 documents (1 valid)", "3 returned", "Pass"],
        ["7", "Urgency order", "Same 4 documents", "Expired first", "Pass"],
        ["8", "Dashboard counts", "Same 4 documents", "1 expired", "Pass"],
        ["9", "Reminder text (expired)", "Expired 3 days ago", "\"... expired 3 days ago\"", "Pass"],
        ["10", "Reminder text (today)", "0 days left", "\"... expires TODAY\"", "Pass"],
        ["11", "Renewal keeps validity length", "1-year policy", "Expiry + 365 days", "Pass"],
        ["12", "Renewal of expired document", "Expired, no issue date", "Today + 1 year", "Pass"],
        ["13", "Offline store persistence", "Insert 2, reopen file", "2 documents", "Pass"],
        ["14", "Offline store ordering", "Reopen file", "Sorted by expiry", "Pass"],
        ["15", "Offline store update", "Edit notes, reopen", "Notes saved", "Pass"],
        ["16", "Offline store delete", "Delete 1, reopen", "1 document", "Pass"],
        ["17", "ID generation after reload", "Insert after reopen", "Next ID = 3", "Pass"],
    ]
    extra = [("BACKGROUND", (4, r), (4, r), GREEN) for r in range(1, len(tc))]
    s.append(table(tc, [0.8 * cm, 4.2 * cm, 5 * cm, 4 * cm, 2.4 * cm], extra=extra))
    s.append(P("Result: 17 passed, 0 failed.", ParagraphStyle("r", parent=body, fontName="Times-Bold", spaceBefore=6)))
    s.append(P("8.2 Validation and Manual Tests", h2))
    s.append(table([
        ["Scenario", "Expected behaviour", "Result"],
        ["Save with empty title", "Warning \"Please enter a title\"; nothing saved", "Pass"],
        ["Issue date after expiry date", "Warning; nothing saved", "Pass"],
        ["Delete document", "Confirmation dialog; removed only on Yes", "Pass"],
        ["Change type on new document", "Reminder days switch to that type's default", "Pass"],
        ["Search \"insurance\"", "Only matching rows shown", "Pass"],
        ["Click \"Renew soon\" card", "Table filtered to due documents", "Pass"],
        ["MySQL server not running", "Error explained; offline storage offered", "Pass"],
        ["Restart in offline mode", "Previously entered documents reload", "Pass"],
    ], [5 * cm, 9 * cm, 2.4 * cm], extra=[("BACKGROUND", (2, r), (2, r), GREEN) for r in range(1, 9)]))

    # 9 Conclusion
    s.append(P("9. Conclusion", h1))
    s.append(P("The Personal Document Expiry Tracker meets its objective of giving users one reliable place to manage "
               "the validity of their important documents. The system calculates the status of every document "
               "automatically, uses a reminder window suited to each type of document, and warns the user through "
               "start-up summaries and desktop notifications, so renewals are not missed. The layered "
               "design with the DAO pattern keeps the user interface, the reminder rules and the storage independent, "
               "which made the logic easy to test and allows the offline fallback. The project shows that a "
               "practical, real-life problem can be solved at no cost with standard technologies such as Java and MySQL."))
    s.append(P("10. Future Scope", h1))
    s.extend(bullets([
        "<b>Email and SMS reminders</b> using JavaMail or an SMS gateway, so alerts reach the user even when the PC is off.",
        "<b>Multi-user login</b> with hashed passwords, so a family can share one database.",
        "<b>Document attachments:</b> store a scan or photo of each document.",
        "<b>Run at Windows start-up</b> so reminders appear without opening the app.",
        "<b>Mobile or web version</b> using the same database through a REST API.",
        "<b>OCR</b> to read the expiry date automatically from a photo of the document.",
    ]))
    s.append(P("11. References", h1))
    for r in [
        "Oracle. <i>The Java Tutorials: Creating a GUI With Swing.</i> docs.oracle.com/javase/tutorial/uiswing",
        "Oracle. <i>Java SE API: java.time package.</i> docs.oracle.com/en/java/javase",
        "Oracle. <i>JDBC Basics.</i> docs.oracle.com/javase/tutorial/jdbc",
        "MySQL. <i>MySQL 8.0 Reference Manual</i> and <i>Connector/J Developer Guide.</i> dev.mysql.com/doc",
        "Gamma, E., Helm, R., Johnson, R., Vlissides, J. <i>Design Patterns: Elements of Reusable Object-Oriented Software.</i> Addison-Wesley.",
        "Sommerville, I. <i>Software Engineering</i>, 10th ed. Pearson.",
    ]:
        s.append(Paragraph(r, ParagraphStyle("ref", parent=bullet, alignment=0), bulletText="•"))
    return s


def main():
    doc = ReportDoc(str(OUT))
    story = cover() + [NextPageTemplate("body"), PageBreak()] + content()
    doc.multiBuild(story)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
