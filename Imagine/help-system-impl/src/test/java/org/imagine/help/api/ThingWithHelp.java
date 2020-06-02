package org.imagine.help.api;

import org.imagine.help.api.annotations.Help;
import org.imagine.help.api.annotations.Help.HelpText;

/**
 *
 * @author Tim Boudreau
 */
@Help(id = "first", content = {
    @HelpText(value = "This is some *help* and its neighbors", topic="What To Do When You Need Help", keywords={"help", "danger"}),
    @HelpText(language = "en", country = "GB", value = "This is some help and its _neighbours_", topic="When You Need Assistance", keywords={"help", "danger"}),
    @HelpText(language = "cs", value = "Tohle je nějaká ~~pomoc~~ a její sousedé", keywords = {"pomoc", "bezpečnost"},
            topic="Co Dělat, Když Potřebujete Pomoc")
})
public class ThingWithHelp {

    @Help(id = "second", content = @HelpText("A second item with *a little help* from my friends!\n"))
    private static String foo;

}
