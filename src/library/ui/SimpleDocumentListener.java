package library.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class SimpleDocumentListener implements DocumentListener {

    private final Runnable onChange;

    SimpleDocumentListener(Runnable onChange) {
        this.onChange = onChange;
    }

    public void insertUpdate(DocumentEvent e) { onChange.run(); }
    public void removeUpdate(DocumentEvent e) { onChange.run(); }
    public void changedUpdate(DocumentEvent e) { onChange.run(); }
}
