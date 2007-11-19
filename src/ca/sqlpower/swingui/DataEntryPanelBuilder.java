/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.swingui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.validation.swingui.FormValidationHandler;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

public class DataEntryPanelBuilder {
	static Logger logger = Logger.getLogger(DataEntryPanelBuilder.class);
	public static final String OK_BUTTON_LABEL = "OK";
	public static final String CANCEL_BUTTON_LABEL = "Cancel";
    
    /**
     * Tries very hard to create a JDialog which is owned by the parent
     * Window of the given component.  However, if the component does not
     * have a Window ancestor, or the component has a Window ancestor that
     * is not a Frame or Dialog, this method instead creates an unparented
     * JDialog which is always-on-top.
     *
     * @param owningComponent The component that should own this dialog.
     * @param title The title for the dialog.
     * @return A JDialog that is
     * @author Jonathan Fuerth (donated to SQL Power on October 31, 2006)
     */
    public static JDialog makeOwnedDialog(Component owningComponent,
            String title) {
        Window owner = SwingUtilities.getWindowAncestor(owningComponent);
        if (owner instanceof Frame) {
            return new JDialog((Frame) owner, title);
        } else if (owner instanceof Dialog) {
            return new JDialog((Dialog) owner, title);
        } else {
            JDialog d = new JDialog();
            d.setTitle(title);
            d.setAlwaysOnTop(true);
            return d;
        }
    }

	/**
	 * Build a JDialog around an object that implements DataEntryPanel, to
	 * provide consistent behaviours such as Cancel button, <ESC> to close, and
	 * so on.
	 * XXX Worry about modal vs non-modal
	 * @param dataEntry
	 *            The DataEntryPanel implementation
	 * @param dialogParent
	 *            A Window object to be the dialog's parent
	 * @param dialogTitle
	 *            The dialog title.
	 * @param actionButtonTitle
	 *            The label text for the OK button
	 * @return The new JDialog, which has the panel in it along with OK and Cancel buttons
	 * @param okCall<Boolean> Call to be invoked when the OK/action button is
	 * 	pressed; does NOT need to dismiss the dialog we will do this if the call returns false
	 * @param cancelCall<Boolean> Call to be invoked when the cancel button is
	 * 	pressed; We will dismiss the dialog if the call returns true
	 * @return
	 */
	public static JDialog createDataEntryPanelDialog(
			final DataEntryPanel dataEntry,
			final Window dialogParent,
			final String dialogTitle,
			final String actionButtonTitle,
			final Callable<Boolean> okCall,
			final Callable<Boolean> cancelCall) {

		final JDialog d = createDialog(dialogParent, dialogTitle);
		JComponent panel = dataEntry.getPanel();


		Action okAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				try {
					boolean close = okCall.call().booleanValue();
					if (close) {
						d.dispose();
					}
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		};
			
		JButton okButton = new JDefaultButton(okAction);
		okButton.setText(actionButtonTitle);
		
		Action closeAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				try {
					boolean close = cancelCall.call().booleanValue();
					if (close) {
						d.dispose();
					}
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		};
		
		//checks if it is a panel that needs to be validated before save.
		if (dataEntry instanceof ValidatedObject) {
			//links the saveAction to the handler
			FormValidationHandler handler = ((ValidatedObject)dataEntry).getHandler();
			handler.setValidatedAction(okAction);
		}
		
		SPSUtils.makeJDialogCancellable(d, closeAction);

		JButton cancelButton = new JButton(closeAction);
		cancelButton.setText(CANCEL_BUTTON_LABEL);
		cancelButton.addActionListener(closeAction);
		cancelButton.addActionListener(new CommonCloseAction(d));

		// Handle if the user presses Enter in the dialog - do OK action
		d.getRootPane().setDefaultButton(okButton);


		// Now build the GUI.
		JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
		cp.add(panel, BorderLayout.CENTER);

		cp.add(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton),
				BorderLayout.SOUTH);
		cp.setBorder(Borders.DIALOG_BORDER);

		//d.add(cp);
		d.setContentPane(cp);

		// XXX maybe pass yet another argument for this?
		// d.setLocationRelativeTo(someFrame);

		d.pack();
		return d;
	}

	/**
	 * Returns a JDialog with the given Window as its parent, and with the given title.
	 * If the dialog parent is neither a Frame or Dialog, it will throw an IllegalArgumentException
	 */
	private static JDialog createDialog(final Window dialogParent, final String dialogTitle) {
		JDialog dialog;
		
		if ( dialogParent == null ) {
            dialog = new JDialog();
            if (logger.isDebugEnabled()) {
            	JOptionPane.showMessageDialog(null,
            	"This action called createDataEntryPanelDialog with DialogParent == null!");
            }
        } else if (dialogParent instanceof Frame) {
			dialog = new JDialog((Frame) dialogParent, dialogTitle);
		} else if (dialogParent instanceof Dialog) {
			dialog = new JDialog((Dialog) dialogParent, dialogTitle);
		} else {
			throw new IllegalArgumentException(
					"The dialogParent you gave me is not a "
							+ "Frame or Dialog (it is a "
							+ dialogParent.getClass().getName() + ")");
		}
		return dialog;
	}

    
    /**
     * Build a JDialog around an object that implements DataEntryPanel, to
     * provide consistent behaviours such as Cancel button, <ESC> to close, and
     * so on.
     *
     * @param dataEntry
     *            The DataEntryPanel implementation
     * @param dialogParent
     *            A Window class to be the parent, or null
     * @param dialogTitle
     *            The display title.
     * @param actionButtonTitle
     *            The title for the OK button
     * @return The built JDialog
     */
    public static JDialog createSingleButtonDataEntryPanelDialog(
    		final DataEntryPanel dataEntry,
    		final Window dialogParent,
    		final String dialogTitle,
    		final String actionButtonTitle) {
    	
    	Action okAction = new AbstractAction() {
    		public void actionPerformed(ActionEvent e) {
    			dataEntry.applyChanges();
    		}
    	};
    	
    	return createSingleButtonDataEntryPanelDialog(dataEntry, dialogParent, dialogTitle,
    			actionButtonTitle, okAction );
    }

	/**
	 * Build a JDialog around an object that implements DataEntryPanel, to
	 * provide consistent behaviours such as Cancel button, <ESC> to close, and
	 * so on.
	 *
	 * @param dataEntry
	 *            The DataEntryPanel implementation
	 * @param dialogParent
	 *            A Window class to be the parent, or null
	 * @param dialogTitle
	 *            The display title.
	 * @param actionButtonTitle
	 *            The title for the OK button
	 * @return The built JDialog
	 */
	public static JDialog createDataEntryPanelDialog(
			final DataEntryPanel dataEntry,
			final Window dialogParent,
			final String dialogTitle,
			final String actionButtonTitle) {

		Callable<Boolean> okCall = new Callable<Boolean>() {
			public Boolean call() {
				return new Boolean(dataEntry.applyChanges());
			}
		};
		
		Callable<Boolean> cancelCall = new Callable<Boolean>() {
			public Boolean call() {
				dataEntry.discardChanges();
				return new Boolean(true);
			}
		};
		
		return createDataEntryPanelDialog(dataEntry, dialogParent, dialogTitle,
				actionButtonTitle, okCall, cancelCall);
	}


    /**
     * Build a JDialog around an object that implements DataEntryPanel, to
     * provide consistent behaviours such as Cancel button, <ESC> to close, and
     * so on.
     * XXX Worry about modal vs non-modal
     * @param dataEntry
     *            The DataEntryPanel implementation
     * @param dialogParent
     *            A Window object to be the dialog's parent
     * @param dialogTitle
     *            The dialog title.
     * @param actionButtonTitle
     *            The label text for the OK button
     * @param okAction Action to be invoked when the OK/action button is
     *  pressed; does NOT need to dismiss the dialog (we do that if applyChanges() returns true).
     * @return The new JDialog, which has the panel in it along with OK and Cancel buttons
     */
    public static JDialog createSingleButtonDataEntryPanelDialog(
            final DataEntryPanel dataEntry,
            final Window dialogParent,
            final String dialogTitle,
            final String actionButtonTitle,
            final Action okAction ) {

    	final JDialog d = createDialog(dialogParent, dialogTitle);
        JComponent panel = dataEntry.getPanel();


        JButton okButton = new JDefaultButton(okAction);
        okButton.setText(actionButtonTitle);
        // In all cases we have to close the dialog.
        Action closeAction = new CommonCloseAction(d);
        okButton.addActionListener(closeAction);
        SPSUtils.makeJDialogCancellable(d, closeAction);
        okButton.addActionListener(new CommonCloseAction(d));

        // Handle if the user presses Enter in the dialog - do OK action
        d.getRootPane().setDefaultButton(okButton);


        // Now build the GUI.
        JPanel cp = new JPanel(new BorderLayout());
        cp.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        cp.add(panel, BorderLayout.CENTER);

        cp.add(ButtonBarFactory.buildCenteredBar(okButton),
                BorderLayout.SOUTH);
        cp.setBorder(Borders.DIALOG_BORDER);

        //d.add(cp);
        d.setContentPane(cp);

        // XXX maybe pass yet another argument for this?
        // d.setLocationRelativeTo(someFrame);

        d.pack();
        return d;
    }
}
