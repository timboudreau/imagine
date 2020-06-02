package org.imagine.help.api.demo;

import org.imagine.help.api.annotations.Help;

/**
 *
 * @author Tim Boudreau
 */
@Help(id = "InheritTopic1", content = {
    @Help.HelpText(value = "Turning right-side up - get *help* from a friend",
            keywords = {"practical", "solutions", "help", "cooperation"})})
public class AnotherThingWithHelpInAnotherPackage {

    @Help(id = "Ih2", content = {
        @Help.HelpText(value = "Tell your friends that _they're_ upside-down!",
                keywords = {"gaslighting", "joke"})})
    void foo() {

    }
}
