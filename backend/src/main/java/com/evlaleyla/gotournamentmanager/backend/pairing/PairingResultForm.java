package com.evlaleyla.gotournamentmanager.backend.pairing;

/**
 * Form object used to transfer a pairing result selection from the UI
 * to the controller layer.
 *
 * <p>The field stores the symbolic result option chosen in the form,
 * for example {@code BLACK_WINS}, {@code WHITE_WINS}, or {@code JIGO}.
 * The controller is responsible for translating this UI value into the
 * internal domain-specific result code.</p>
 */
public class PairingResultForm {

    /**
     * Selected result option submitted by the user interface.
     *
     * <p>This is intentionally stored as a UI-oriented value and is later
     * converted into the internal result representation in the controller.</p>
     */
    private String resultOption;

    /**
     * Default constructor required for Spring form binding.
     */
    public PairingResultForm() {
    }

    /**
     * Returns the selected result option from the form submission.
     *
     * @return the selected UI result option
     */
    public String getResultOption() {
        return resultOption;
    }

    /**
     * Sets the selected result option from the form submission.
     *
     * @param resultOption the selected UI result option
     */
    public void setResultOption(String resultOption) {
        this.resultOption = resultOption;
    }
}