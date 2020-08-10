package org.imagine.help.annotation.processors;

import com.mastfrog.annotation.AnnotationUtils;
import com.mastfrog.java.vogon.ClassBuilder;
import com.mastfrog.util.collections.CollectionUtils;
import static com.mastfrog.util.collections.CollectionUtils.setOf;
import com.mastfrog.util.strings.Strings;
import java.io.IOException;
import java.io.OutputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;
import static org.imagine.help.annotation.processors.HelpAnnotationProcessor.HELP_ANNO;
import static org.imagine.help.annotation.processors.HelpAnnotationProcessor.HELP_TEXT_INNER_ANNO;
import static org.imagine.help.annotation.processors.HelpAnnotationProcessor.TOPIC_ANNO;
import static org.imagine.help.annotation.processors.HelpAnnotationProcessor.TOPIC_LOC_INNER_ANNO;
import org.imagine.markdown.uiapi.ErrorConsumer;
import org.imagine.markdown.uiapi.ErrorConsumer.ProblemKind;
import org.imagine.markdown.uiapi.Markdown;
import com.mastfrog.util.strings.LevenshteinDistance;

/**
 * Generates an enum constant for each &#064;Help annotation found, an an enum
 * called HelpItems in the same package as the annotated class.
 *
 * @author Tim Boudreau
 */
@SupportedOptions(AnnotationUtils.AU_LOG)
// If we want to provide completion for the inner annotations, which have no retention
// and will never be passed in, we need to declare them here anyway
@SupportedAnnotationTypes({HELP_ANNO, TOPIC_ANNO, HELP_TEXT_INNER_ANNO, TOPIC_LOC_INNER_ANNO})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
/*
 * Using a flat META-INF/services file instead, to avoid maven becoming confused
 * as to whether org-openide-util-lookup is in scope provided or not.
 */
//@ServiceProvider(service = Processor.class)
public class HelpAnnotationProcessor extends AbstractProcessor {

    public static final String HELP_ANNO = "org.imagine.help.api.annotations.Help";
    public static final String HELP_TEXT_INNER_ANNO = "org.imagine.help.api.annotations.Help$HelpText";
    public static final String TOPIC_ANNO = "org.imagine.help.api.annotations.Topic";
    public static final String TOPIC_LOC_INNER_ANNO = "org.imagine.help.api.annotations.Topic$Loc";
    private AnnotationUtils utils;
    private BiPredicate<? super AnnotationMirror, ? super Element> test;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        utils = new AnnotationUtils(processingEnv, setOf(HELP_ANNO), HelpAnnotationProcessor.class);
        super.init(processingEnv);
        test = utils.multiAnnotations().whereAnnotationType(HELP_ANNO, b -> {
            b.testMember("id").stringValueMustBeValidJavaIdentifier().build()
                    .testMember("related").stringValueMustMatch(
                    (idText, onError) -> {
                        if (idText == null) {
                            return true;
                        }
                        String[] parts = idText.split("\\.");
                        boolean failed = false;
                        for (String part : parts) {
                            if (part.isEmpty()) {
                                onError.accept("Leading . or empty identifier / consecutive . "
                                        + "characters in related help item identifier '" + idText + "'");
                                failed = true;
                            }
                            for (int i = 0; i < part.length(); i++) {
                                if (i == 0 && !Character.isJavaIdentifierStart(part.charAt(0))) {
                                    onError.accept("'" + part.charAt(0) + "' is not a legal first "
                                            + "character in a java identifier, in related help item "
                                            + "identifier '" + idText + "'");
                                    failed = true;
                                } else if (!Character.isJavaIdentifierPart(part.charAt(0))) {
                                    onError.accept("'" + part.charAt(0) + "' is not a legal character "
                                            + "in a java identifier,  in related help item identifier '"
                                            + idText + "'");
                                    failed = true;
                                }
                            }
                        }
                        return !failed;
                    }).build()
                    .testMemberAsAnnotation("content", amtb -> {
                        amtb.testMember("language").stringValueMustMatch((language, onError) -> {
                            if (language == null) {
                                // default en
                                return true;
                            }
                            Set<String> languages = setOf(Locale.getISOLanguages());
                            if (!languages.contains(language)) {
                                onError.accept("Not an ISO 639 language code: '" + language + "'");
                                return false;
                            }
                            return true;
                        }).build();
                        amtb.testMember("country").stringValueMustMatch((country, onError) -> {
                            if (country == null || country.isEmpty()) {
                                // default US
                                return true;
                            }
                            Set<String> countries = setOf(Locale.getISOCountries());
                            if (!countries.contains(country)) {
                                onError.accept("Not an ISO 639 country: '" + country + "'");
                                return false;
                            }
                            return true;
                        }).build();
                        amtb.testMember("value").stringValueMustMatch((markdownText, onError) -> {
                            Markdown md = new Markdown(markdownText);
                            boolean[] result = new boolean[]{true};
                            md.checkErrors((ErrorConsumer.ProblemKind kind, int startOffset, int endOffset, String offending, String msg) -> {
                                if (kind.isFatal()) {
                                    onError.accept(msg);
                                    result[0] = false;
                                } else {
                                    if (kind == ProblemKind.ATTEMPT_FULL_CONTEXT) {
                                        return;
                                    }
                                    utils.warn(msg);
                                }
                            });
                            return true;
                        }).build();
                    }).build();
        }).whereAnnotationType(TOPIC_ANNO, b -> {
            b.testMemberAsAnnotation("value", amtb -> {
                amtb.testMember("language").stringValueMustMatch((language, onError) -> {
                    if (language == null) {
                        // default en
                        return true;
                    }
                    Set<String> languages = setOf(Locale.getISOLanguages());
                    if (!languages.contains(language)) {
                        onError.accept("Not an ISO 639 language code: '" + language + "'");
                        return false;
                    }
                    return true;
                }).build();
                amtb.testMember("country").stringValueMustMatch((country, onError) -> {
                    if (country == null || country.isEmpty()) {
                        // default US
                        return true;
                    }
                    Set<String> countries = setOf(Locale.getISOCountries());
                    if (!countries.contains(country)) {
                        onError.accept("Not an ISO 639 country: '" + country + "'");
                        return false;
                    }
                    return true;
                }).build();
                amtb.testMember("value").stringValueMustMatch((topicText, onError) -> {
                    if (topicText == null || topicText.trim().isEmpty()) {
                        onError.accept("Topic text is empty");
                        return false;
                    }
                    return true;
                }).build();
            }).build();
        }).build();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        GenerationResult res = new GenerationResult();
        Element lastElement = null;
        try {
            for (Element el : utils.findAnnotatedElements(roundEnv, HELP_ANNO, TOPIC_ANNO)) {
                lastElement = el;
                Set<AnnotationMirror> mirrors = utils.findAnnotationMirrors(el, HELP_ANNO, TOPIC_ANNO);
                for (AnnotationMirror mir : mirrors) {
                    if (test.test(mir, el)) {
                        if (mir.getAnnotationType().toString().equals(HELP_ANNO)) {
                            handleHelpAnnotation(el, mir, roundEnv);
                        } else if (mir.getAnnotationType().toString().equals(TOPIC_ANNO)) {
                            handleTopicAnnotation(el, mir, roundEnv);
                        }
                    }
                }
            }
            // XXX we really *should* do this at the final round, however,
            // that prevents all @ServiceProvider annotations from being
            // processed.  So either we do our own generation of META-INF/services
            // files, which could result in the filer complaining that the file was
            // opened twice, or we just pray another annotation processor never
            // generates @Help annotations

//            if (roundEnv.processingOver() && !roundEnv.errorRaised()) {
            generate(res);
//            }
        } catch (Exception | Error ex) {
            ex.printStackTrace(System.err);
            utils.fail(ex.toString(), lastElement);
        }
        // If we return TRUE here after generating indices, then the
        // @ServiceProvider annotations on generated HIndex classes will
        // never be processed
//        return !res.indicesGenerated();
//        return false;
        return true;
    }

    private final Map<String, TopicInfo> topicForElement = new HashMap<>();

    private void handleTopicAnnotation(Element el, AnnotationMirror mir, RoundEnvironment roundEnv) {
        List<AnnotationMirror> items = utils.annotationValues(mir, "value", AnnotationMirror.class);
        if (items.isEmpty()) {
            utils.warn("Topic annotation present but has no topic entries", el, mir);
        }
        String typeOrPackageName = typeOrPackageNameOf(el);
        TopicInfo info = topicForElement.get(typeOrPackageName);
        if (info == null) {
            info = new TopicInfo(el);
            topicForElement.put(typeOrPackageName, info);
        }
        for (AnnotationMirror am : items) {
            String language = utils.annotationValue(am, "language", String.class, "en");
            String country = utils.annotationValue(am, "country", String.class, "en".equals(language) ? "US" : "");
            String text = utils.annotationValue(am, "value", String.class);
            info.add(language, country, text);
        }
    }

    private void handleHelpAnnotation(Element el, AnnotationMirror mir, RoundEnvironment roundEnv) {

        List<AnnotationMirror> texts = utils.annotationValues(mir, "content", AnnotationMirror.class);
        String id = utils.annotationValue(mir, "id", String.class);
        boolean noIndex = utils.annotationValue(mir, "noIndex", Boolean.class, Boolean.FALSE);
        List<String> related = utils.annotationValues(mir, "related", String.class);
        boolean publyc = utils.annotationValue(mir, "makePublic", Boolean.class, Boolean.FALSE);
        for (AnnotationMirror am : texts) {
            String language = utils.annotationValue(am, "language", String.class, "en");
            String country = utils.annotationValue(am, "country", String.class, "en".equals(language) ? "US" : "");
            String text = utils.annotationValue(am, "value", String.class);
            String topic = utils.annotationValue(am, "topic", String.class);
            List<String> keywords = utils.annotationValues(am, "keywords", String.class);
            if (text == null) {
                utils.fail("Null text on " + am, el, mir);
                return;
            }
            handleOneHelpLocale(el, mir, roundEnv, id, language, country, text, topic, keywords, noIndex, related, publyc);
        }
    }

    private final Map<String, Set<HelpInfo>> helpInfosForPackage = CollectionUtils.supplierMap(TreeSet::new);

    private void handleOneHelpLocale(Element el, AnnotationMirror mir, RoundEnvironment roundEnv, String id, String locale, String variant, String text, String topic, List<String> keywords, boolean noIndex, List<String> related, boolean publyc) {
        String packageName = utils.packageName(el);
        HelpInfo info = getOrCreateHelpInfo(packageName, id, noIndex, related, publyc);
        OneLocaleInfo one = info.add(el, locale, variant, text, topic, keywords);
        if (one == null) {
            utils.fail("Help '" + id + "' for " + locale + (variant.isEmpty() ? "" : "-" + variant)
                    + " is present more than once in package " + packageName, el, mir);
        }
    }

    private HelpInfo getOrCreateHelpInfo(String packageName, String id, boolean noIndex, List<String> related, boolean publyc) {
        Set<HelpInfo> forPackage = helpInfosForPackage.get(packageName);
        for (HelpInfo info : forPackage) {
            if (id.equals(info.id)) {
                return info;
            }
        }
        HelpInfo info = new HelpInfo(id, noIndex, related, publyc);
        forPackage.add(info);
        return info;
    }

    private boolean isFullyQualifiedId(String id) {
        if (id.indexOf('.') > 0) {
            return true;
        }
        return false;
    }

    private int idUsageCount(String id) {
        int count = 0;
        for (Map.Entry<String, Set<HelpInfo>> e : helpInfosForPackage.entrySet()) {
            for (HelpInfo info : e.getValue()) {
                if (info.id.equals(id)) {
                    count++;
                }
            }
        }
        return count;
    }

    private String qualify(String projectRelativeRelatedId) throws AmbiguousIdException {
        int usages = idUsageCount(projectRelativeRelatedId);
        if (usages == 1) {
            for (Map.Entry<String, Set<HelpInfo>> e : helpInfosForPackage.entrySet()) {
                for (HelpInfo info : e.getValue()) {
                    if (projectRelativeRelatedId.equals(info.id)) {
                        return e.getKey() + '.' + projectRelativeRelatedId;
                    }
                }
            }
        } else if (usages > 1) {
            throw new AmbiguousIdException(projectRelativeRelatedId);
        }
        return null;
    }

    private Set<String> possibleQualifiedNames(String id) {
        Set<String> result = new TreeSet<>();
        for (Map.Entry<String, Set<HelpInfo>> e : helpInfosForPackage.entrySet()) {
            for (HelpInfo info : e.getValue()) {
                if (id.equals(info.id)) {
                    result.add(e.getKey() + '.' + id);
                }
            }
        }
        return result;
    }

    private Set<String> allKnownIds() {
        Set<String> result = new TreeSet<>();
        for (Map.Entry<String, Set<HelpInfo>> e : helpInfosForPackage.entrySet()) {
            for (HelpInfo info : e.getValue()) {
                if (info.id != null) { // half written annotation
                    result.add(info.id);
                }
            }
        }
        return result;
    }

    public HelpInfo publicOwnerOfId(String id) {
        boolean qual = isFullyQualifiedId(id);
        for (Map.Entry<String, Set<HelpInfo>> e : helpInfosForPackage.entrySet()) {
            for (HelpInfo info : e.getValue()) {
                if (!qual) {
                    if (id.equals(info.id) && info.generatePublicClass()) {
                        return info;
                    }
                } else if (id.equals(e.getKey() + "." + info.id) && info.generatePublicClass()) {
                    return info;
                }
            }
        }
        return null;
    }

    private String nearestMatchesString(String id) {
        List<String> all = new ArrayList<>(allKnownIds());
        if (all.isEmpty()) {
            return null;
        }
        LevenshteinDistance.sortByDistance(id, all);
        StringBuilder sb = new StringBuilder();
        for (String top : all) {
            float score = LevenshteinDistance.score(id, top, false);
            if (score < 0.45) {
                if (sb.length() != 0) {
                    sb.append(" or ");
                }
                sb.append('\'').append(top).append('\'');
            } else {
                break;
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    class AmbiguousIdException extends Exception {

        private final String id;

        AmbiguousIdException(String id) {
            super(id);
            this.id = id;
        }
    }

    private void generate(GenerationResult res) {
        if (!helpInfosForPackage.isEmpty()) {
            for (Map.Entry<String, Set<HelpInfo>> e : helpInfosForPackage.entrySet()) {
                String pkg = e.getKey();
                generateOnePackage(pkg, e.getValue(), res);
            }
            helpInfosForPackage.clear();
        }
    }

    static class GenerationResult {

        private boolean generated;
        private boolean indicesGenerated;

        void generating() {
            generated = true;
        }

        void generatingIndex() {
            indicesGenerated = true;
        }

        boolean codeGenerated() {
            return generated;
        }

        boolean indicesGenerated() {
            return indicesGenerated;
        }
    }

    private void generateOnePackage(String pkg, Set<HelpInfo> infos, GenerationResult res) {
        if (infos.isEmpty()) {
            return;
        }
        boolean hasContent = false;
        int indexable = 0;
        boolean publyc = false;
        Set<Element> allElements = new HashSet<>();
        Set<String> related = new HashSet<>();
        for (HelpInfo ifo : infos) {
            if (!ifo.locales.isEmpty()) {
                hasContent = true;
            }
            publyc |= ifo.generatePublicClass();
            related.addAll(ifo.related);
            allElements.addAll(ifo.elements);
            if (ifo.isIndexable()) {
                indexable++;
            }
        }
        if (!hasContent) {
            return;
        }
//        Locale loc = Locale.getDefault();
        String generatedClassName = "HelpItems";
        String generatedFqn = pkg + "." + generatedClassName;
        ClassBuilder<String> cb = ClassBuilder.forPackage(pkg).named(generatedClassName)
                .importing("org.imagine.help.api.HelpItem", "org.imagine.help.implspi.HelpLoader", "java.util.Locale")
                .implementing("HelpItem").toEnum()
                .iteratively(infos, (bldr, info) -> {
                    bldr.enumConstants().add(info.id).endEnumConstants();
                })
                .overridePublic("heading", mb -> {
                    mb.addArgument("Locale", "locale").returning("String").body().returningInvocationOf("heading")
                            .withArgument("this").withArgument("locale").onInvocationOf("getDefault")
                            .on("HelpLoader").endBlock();
                })
                .overridePublic("asPlainText", mb -> {
                    mb.addArgument("Locale", "locale").returning("String").body().returningInvocationOf("fullText")
                            .withArgument("this").withArgument("locale").onInvocationOf("getDefault")
                            .on("HelpLoader").endBlock();
                })
                .overridePublic("topic", mb -> {
                    mb.addArgument("Locale", "locale").returning("String").body(bb -> {
                        bb.declare(VAR_LANGUAGE).initializedByInvoking("getLanguage").on("locale").as("String");
                        bb.declare(VAR_COUNTRY).initializedByInvoking("getCountry").on("locale").as("String");
                        bb.switchingOn("this", idSwitch -> {
                            boolean[] anyGenerated = new boolean[1];
                            for (HelpInfo ifo : infos) {
                                if (ifo.infosByLanguage.isEmpty()) {
                                    continue;
                                }
                                idSwitch.inCase(ifo.id, idBlock -> {
                                    // In the case we error out, we still need to
                                    // generate the switch block
                                    anyGenerated[0] = true;
                                    idBlock.switchingOn(VAR_LANGUAGE, languageSwitch -> {
                                        boolean[] anyLanguages = new boolean[1];
                                        ifo.visitLanguagesAndInfos((lang, countries) -> {
                                            if (lang.isEmpty()) {
                                                if (ifo.elements.isEmpty()) {
                                                    utils.fail("Empty language tag for " + ifo.id);
                                                } else {
                                                    utils.fail("Empty language tag", ifo.elements.iterator().next());
                                                }
                                                return;
                                            }
                                            languageSwitch.inStringLiteralCase(lang, langCase -> {
                                                anyLanguages[0] = true;
                                                if (countries.size() == 1) {
                                                    langCase.returningStringLiteral(topicFor(countries.iterator().next()));
                                                } else {
                                                    langCase.switchingOn(VAR_COUNTRY, varSwitch -> {
                                                        for (OneLocaleInfo var : countries) {
                                                            varSwitch.inStringLiteralCase(var.country, varBlock -> {
                                                                varBlock.returningStringLiteral(topicFor(var));
                                                            });
                                                        }
                                                        OneLocaleInfo fallback = ifo.fallback();
                                                        if (fallback != null) {
                                                            varSwitch.inDefaultCase(defBlock -> {
                                                                defBlock.log(Level.FINEST).argument(VAR_COUNTRY)
                                                                        .argument("name()")
                                                                        .stringLiteral(fallback.country)
                                                                        .logging("No country {0} for {1} - using fallback topic {2}");
                                                                defBlock.returningStringLiteral(topicFor(fallback));
                                                            });
                                                        }
                                                    });
                                                }
                                            });
                                        });
                                        if (!anyLanguages[0]) {
                                            // XXX probably SwitchBuilder should fail the build, but
                                            // generate an empty default block by default
                                            languageSwitch.inDefaultCase(ib -> {
                                                ib.lineComment("// Annotation processor errored out.");
                                                ib.lineComment("// This clause exists just to generate valid code.");
                                                ib.lineComment("// The build has already failed.");
                                            });
                                        }
                                    });
                                    idBlock.statement("break");
                                });
                            }
                            if (!anyGenerated[0]) {
                                idSwitch.inDefaultCase(ib -> {
                                    ib.lineComment("// Annotation processor errored out.");
                                    ib.lineComment("// This clause exists just to generate valid code.");
                                    ib.lineComment("// The build has already failed.");
                                });
                            }
                        });
                        bb.returningNull();
                    });
                })
                .overridePublic("getContent", mb -> {
                    mb.withTypeParam("T")
                            .returning("T")
                            .addArgument("Class<T>", "type")
                            .addArgument(Locale.class.getName(), "locale")
                            .body(bb -> {
                                bb.lineComment("PENDING: The annotation processor should save text as resources on");
                                bb.lineComment("the classpath rather than embed full text to reduce class size");
                                bb.declare(VAR_LANGUAGE).initializedByInvoking("getLanguage").on("locale").as("String");
                                bb.declare(VAR_COUNTRY).initializedByInvoking("getCountry").on("locale").as("String");
                                bb.log(Level.FINE).argument(VAR_LANGUAGE).argument(VAR_COUNTRY)
                                        .argument("name()").argument("locale")
                                        .logging("Request for {2} in locale ''{0}'' variant ''{1}'' ({3})");
                                boolean[] anyGenerated = new boolean[1];
                                bb.switchingOn("this", idSwitch -> {
                                    for (HelpInfo ifo : infos) {
                                        if (ifo.infosByLanguage.isEmpty()) {
                                            continue;
                                        }
                                        idSwitch.inCase(ifo.id, idBlock -> {
                                            anyGenerated[0] = true;
                                            idBlock.switchingOn(VAR_LANGUAGE, languageSwitch -> {
                                                ifo.visitLanguagesAndInfos((lang, variants) -> {
                                                    languageSwitch.inStringLiteralCase(lang, langCase -> {
                                                        if (variants.size() == 1) {
                                                            langCase.returningInvocationOf("load")
                                                                    .withArgument("type").withArgumentFromInvoking("name").inScope()
                                                                    .withStringLiteral(variants.iterator().next().text)
                                                                    .onInvocationOf("getDefault")
                                                                    .on("HelpLoader");
                                                        } else {
                                                            langCase.switchingOn(VAR_COUNTRY, varSwitch -> {
                                                                for (OneLocaleInfo var : variants) {
                                                                    varSwitch.inStringLiteralCase(var.country, varBlock -> {
                                                                        varBlock.returningInvocationOf("load")
                                                                                .withArgument("type").withArgumentFromInvoking("name").inScope()
                                                                                .withStringLiteral(var.text)
                                                                                .onInvocationOf("getDefault")
                                                                                .on("HelpLoader");
                                                                    });
                                                                }
                                                                OneLocaleInfo fallback = ifo.fallback();
                                                                if (fallback != null) {
                                                                    varSwitch.inDefaultCase(defBlock -> {
                                                                        defBlock.log(Level.FINEST).argument(VAR_COUNTRY)
                                                                                .argument("name()")
                                                                                .stringLiteral(fallback.country)
                                                                                .logging("No country {0} for {1} - using fallback {2}");
                                                                        defBlock.returningInvocationOf("load")
                                                                                .withArgument("type").withArgumentFromInvoking("name").inScope()
                                                                                .withStringLiteral(fallback.text)
                                                                                .onInvocationOf("getDefault")
                                                                                .on("HelpLoader");
                                                                    });
                                                                }
                                                            });
                                                        }
                                                    });
                                                });
                                            });
                                            idBlock.statement("break");
                                        });
                                    }
                                    if (!anyGenerated[0]) {
                                        idSwitch.inDefaultCase(ib -> {
                                            ib.lineComment("// Annotation processor errored out.");
                                            ib.lineComment("// This clause exists just to generate valid code.");
                                            ib.lineComment("// The build has already failed.");
                                        });
                                    }
                                });
                                bb.returningNull();
                            });
                });
        if (publyc) {
            cb.makePublic();
        }
        if (!related.isEmpty()) {
            // Related topics are resolved one of a few ways, since we need to return a list
            // of viable HelpItem instances.
            // 1. The ID is unqualified, local to this package and used on the same enum - just return it
            // 2.  The ID is unqualified, NOT local to this package, reachable within the compilation unit
            // and unique
            // 3.  The ID is fully qualified package-name.dot.enum-id, and HelpInfo.resolve can
            // find it
            cb.importing("java.util.List", "java.util.ArrayList").overridePublic("related", mb -> {
                mb.returning("List<? extends HelpItem>").body(bb -> {
                    Set<String> idsInThisPackage = new HashSet<>();
                    for (HelpInfo ifo : infos) {
                        idsInThisPackage.add(ifo.id);
                    }
                    List<HelpInfo> all = new ArrayList<>();
                    for (HelpInfo ifo : infos) {
                        boolean ok = true;
                        for (String rel : ifo.related) {
                            if (rel.equals(ifo.id)) {
                                utils.fail("Related items should not contain self",
                                        ifo.elements.iterator().hasNext() ? ifo.elements.iterator().next() : null);
                                ok = false;
                            } else {
                                // XXX this fails for incremental compilation in an IDE.
                                // Need to generate a file listing the items in meta-inf or something
                                // to have an index to check to make this work incrementally
                                /*
                                if (!idsInThisPackage.contains(rel) && !isFullyQualifiedId(rel)) {
                                    try {
                                        String q = this.qualify(rel);
                                        if (q == null) {
                                            String matches = nearestMatchesString(rel);
                                            if (matches != null) {
                                                utils.fail("Unresolvable related help item '" + rel + "' on '" + ifo.id + "'. "
                                                        + "Perhaps you mean " + matches,
                                                        ifo.elements.iterator().hasNext() ? ifo.elements.iterator().next() : null);

                                            } else {
                                                utils.fail("Unresolvable related help item '" + rel + "' on '" + ifo.id + "'",
                                                        ifo.elements.iterator().hasNext() ? ifo.elements.iterator().next() : null);
                                            }
//                                            ok = false;
                                        }
                                    } catch (AmbiguousIdException ex) {
                                        ok = false;
                                        Set<String> options = possibleQualifiedNames(rel);
                                        StringBuilder msg = new StringBuilder("Related item '")
                                                .append(rel).append("' is ambiguous and could "
                                                + "reference any of: ");

                                        for (Iterator<String> it = options.iterator(); it.hasNext();) {
                                            msg.append(it.next());
                                            if (it.hasNext()) {
                                                msg.append(", ");
                                            }
                                        }
                                        msg.append(". Replace the unqualified ID with one of these "
                                                + "qualified ones, or rename all but one of them so the "
                                                + "id is unique.");
                                        ifo.elements.forEach(e -> {
                                            utils.fail(msg.toString(), e);
                                        });

                                    }
                                }
                                */
                            }
                        }
                        if (ok) {
                            all.add(ifo);
                        }
                    }
                    bb.declare("result").initializedWithNew().withArgument(idsInThisPackage.size())
                            .ofType("ArrayList<>").as("List<HelpItem>");
                    bb.declare("item").as("HelpItem");
                    bb.lineComment("Resolve items local to this package:");
                    bb.lineComment(Strings.join(',', idsInThisPackage));
                    boolean[] anyGenerated = new boolean[1];
                    bb.switchingOn("this", sw -> {
                        for (HelpInfo ifo : all) {
                            if (!ifo.related.isEmpty()) {
                                sw.inCase(ifo.id, idCase -> {
                                    anyGenerated[0] = true;
                                    List<String> unqualified = new ArrayList<>(ifo.related.size());
                                    List<String> qualified = new ArrayList<>(ifo.related.size());
                                    List<String> local = new ArrayList<>(ifo.related.size());
                                    ifo.related.forEach(rel -> {
                                        if (isFullyQualifiedId(rel)) {
                                            // Where the type is public, use a direct
                                            // field reference rather than incur the overhead
                                            // of querying all HelpIndex instances in the system
                                            // (though that could be optimized)
                                            HelpInfo owner = publicOwnerOfId(rel);
                                            if (owner != null) {
                                                int ix = rel.lastIndexOf('.');
                                                String rpkg = rel.substring(0, ix);
                                                String rid = rel.substring(ix + 1);
                                                local.add(rpkg + ".HelpItems." + rid);
                                            } else {
                                                qualified.add(rel);
                                            }
                                        } else if (idsInThisPackage.contains(rel)) {
                                            local.add(rel);
                                        } else {
                                            unqualified.add(rel);
                                        }
                                    });
                                    Collections.sort(unqualified);
                                    unqualified.forEach(id -> {
                                        try {
                                            String q = qualify(id);
                                            if (q == null) {
                                                String nearMatches = nearestMatchesString(id);
                                                String errorMessage = nearMatches == null
                                                        ? "Unknown id '" + id + "' "
                                                        : "Unknown id '" + id + "' - perhaps you mean "
                                                        + nearMatches;
                                                for (Element e : ifo.elements) {
                                                    utils.fail(errorMessage, e);
                                                }
                                            } else {
                                                // Where the type is public, use a direct
                                                // field reference rather than incur the overhead
                                                // of querying all HelpIndex instances in the system
                                                // (though that could be optimized)
                                                HelpInfo owner = publicOwnerOfId(q);
                                                if (owner != null) {
                                                    int ix = q.lastIndexOf('.');
                                                    String rpkg = q.substring(0, ix);
                                                    String rid = q.substring(ix + 1);
                                                    local.add(rpkg + ".HelpItems." + rid);
                                                } else {
                                                    qualified.add(q);
                                                }
                                            }
                                        } catch (AmbiguousIdException ex) {
                                            Set<String> options = possibleQualifiedNames(id);
                                            StringBuilder msg = new StringBuilder("Related item '")
                                                    .append(id).append("' is ambiguous and could "
                                                    + "reference any of: ");

                                            for (Iterator<String> it = options.iterator(); it.hasNext();) {
                                                msg.append(it.next());
                                                if (it.hasNext()) {
                                                    msg.append(", ");
                                                }
                                            }
                                            msg.append(". Replace the unqualified ID with one of these "
                                                    + "qualified ones, or rename all but one of them so the "
                                                    + "id is unique.");
                                            ifo.elements.forEach(e -> {
                                                utils.fail(msg.toString(), e);
                                            });
                                        }
                                    });
                                    Collections.sort(local);
                                    for (String id : local) {
                                        idCase.invoke("add").withArgument(id).on("result");
                                        int count = idUsageCount(id);
                                        if (count > 1) {
                                            utils.warn("Help ID '" + id + "' is used more than once in "
                                                    + "this project; using the help ID defined in this package, "
                                                    + "but ambiguous IDs are can produce confusing results.",
                                                    ifo.elements.iterator().next());
                                        }
                                    }
                                    Collections.sort(qualified);
                                    if (!qualified.isEmpty()) {
                                        cb.importing("org.imagine.help.api.search.HelpIndex");
                                        for (String id : qualified) {
                                            idCase.assign("item").toInvocation("find")
                                                    .withStringLiteral(id)
                                                    .on("HelpItem");
                                            idCase.ifNotNull("item").invoke("add").withArgument("item").on("result")
                                                    .orElse(ecb -> {
                                                        ecb.log(Level.WARNING).logging(generatedFqn + " lists "
                                                                + "a related help item '" + id + "' but no such help "
                                                                + "item was found");

                                                    });
                                        }
                                    }
                                    idCase.statement("break");
                                });
                            }
                        }
                        if (!anyGenerated[0]) {
                            sw.inDefaultCase(ib -> {
                                ib.lineComment("// Annotation processor errored out.");
                                ib.lineComment("// This clause exists just to generate valid code.");
                                ib.lineComment("// The build has already failed.");
                            });
                        }
                    });
                    bb.returning("result");
                });
            });
        }
        ClassBuilder<String> helpIndexImpl = null;
        if (indexable > 0) {
            helpIndexImpl = ClassBuilder.forPackage(pkg).named("HIndex")
                    .extending("AbstractHelpIndex<" + generatedClassName + ">")
                    .importing(generatedFqn,
                            "org.imagine.help.api.search.AbstractHelpIndex",
                            "org.openide.util.lookup.ServiceProvider",
                            "org.imagine.help.api.search.HelpIndex",
                            "org.imagine.help.api.HelpItem",
                            "java.util.Locale"
                    )
                    .withModifier(FINAL, PUBLIC)
                    .annotatedWith("ServiceProvider", ab -> {
                        ab.addClassArgument("service", "HelpIndex");
                    })
                    .constructor(con -> {
                        con.setModifier(PUBLIC)
                                .body(cbody -> {
                                    cbody.invoke("super").withArgument(generatedClassName + ".class")
                                            .inScope();
                                });
                    })
                    .protectedMethod("init").body(bb -> {
                Set<String> added = new HashSet<>(5);
                for (HelpInfo info : infos) {
                    if (info.isIndexable()) {
                        info.visitLanguagesAndInfos((lang, oneLocales) -> {
                            oneLocales.forEach(oneLocale -> {
                                if (oneLocale.languageTag().trim().isEmpty()) {
                                    return;
                                }
                                String localeVar = oneLocale.localeVariableName();
                                String languageTag = oneLocale.languageTag();
//                                String localeVar = oneLocale.language + "_" + oneLocale.country;
                                if (!added.contains(localeVar)) {
                                    bb.declare(localeVar).initializedByInvoking("forLanguageTag")
                                            .withStringLiteral(languageTag)
                                            .on("Locale").as("Locale");
                                    added.add(localeVar);
                                }
                            });
                        });
                    }
                }
                for (HelpInfo help : infos) {
                    // add(Locale language, String topic, String constantName, String[] keywords)
                    if (help.isIndexable()) {
                        for (OneLocaleInfo loc : help) {
                            if (loc.languageTag().trim().isEmpty()) {
                                return;
                            }
                            bb.lineComment(fqnOf(loc.originatingElement));
                            ClassBuilder.InvocationBuilder<?> addInvocation = bb
                                    .invoke("add").withArgument(loc.localeVariableName())
                                    .withStringLiteral(topicFor(loc)).withStringLiteral(help.id);
                            for (String kwd : loc.keywords()) {
                                addInvocation.withStringLiteral(kwd);
                            }
                            addInvocation.inScope();
                        }
                    }
                }
            }).overrideProtected("resolve", mb -> {
                mb.addArgument("String", "pkg").addArgument("String", "id")
                        .returning("HelpItem")
                        .body(bb -> {
                            bb.iff(cbb -> {
                                cbb.invoke("equals").withArgument("pkg").on('"' + pkg + '"').equals().expression("true")
                                        .endCondition(ifEqual -> {
                                            ifEqual.switchingOn("id", idSwitch -> {
                                                for (HelpInfo ifo : infos) {
                                                    idSwitch.inStringLiteralCase(ifo.id, ieCase -> {
                                                        ieCase.returning("HelpItems." + ifo.id);
                                                    });
                                                }
                                            });
                                        });
                            });
                            bb.returningNull();
                        });
            });
        }
        Filer filer = processingEnv.getFiler();
        try {
            JavaFileObject src = filer.createSourceFile(generatedFqn, allElements.toArray(new Element[allElements.size()]));
            try (OutputStream out = src.openOutputStream()) {
                out.write(cb.build().getBytes(UTF_8));
                res.generating();
            }
            if (helpIndexImpl != null) {
                src = filer.createSourceFile(helpIndexImpl.fqn(), allElements.toArray(new Element[allElements.size()]));
                try (OutputStream out = src.openOutputStream()) {
                    res.generatingIndex();
                    out.write(helpIndexImpl.build().getBytes(UTF_8));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            for (Element el : allElements) {
                utils.fail(ex.getMessage(), el);
            }
        }
    }
    private static final String VAR_LANGUAGE = "language";
    private static final String VAR_COUNTRY = "country";

    private Set<Completion> idCompletions(String text) {
        Set<Completion> result = new LinkedHashSet<>();
        text = stripQuotes(text);
        boolean noText = text == null || text.trim().isEmpty();
        Set<HelpInfo> used = new HashSet<>();
        for (Map.Entry<String, Set<HelpInfo>> e : this.helpInfosForPackage.entrySet()) {
            for (HelpInfo info : e.getValue()) {
                String id = info.id;
                if (noText || id.startsWith(text)) {
                    int ct = idUsageCount(id);
                    if (ct == 1) {
                        result.add(new GenericCompletion("'" + id + "' defined on " + fqnOf(info.elements.iterator().next()),
                                noText ? id : id.substring(text.length())));
                        used.add(info);
                    }
                }
            }
        }
        for (Map.Entry<String, Set<HelpInfo>> e : this.helpInfosForPackage.entrySet()) {
            for (HelpInfo info : e.getValue()) {
                String id = info.id;
                if ((noText || e.getKey().startsWith(text)) && !used.contains(info)) {
                    String qid = e.getKey() + "." + id;
                    result.add(new GenericCompletion("'" + id + "' defined on " + fqnOf(info.elements.iterator().next()),
                            noText ? qid : qid.substring(text.length())));
                    used.add(info);
                }
            }
        }
        return result;
    }

    static class GenericCompletion implements Completion {

        private final String msg;
        private final String completion;

        public GenericCompletion(String msg, String completion) {
            this.msg = msg;
            this.completion = completion;
        }

        @Override
        public String getValue() {
            return completion;
        }

        @Override
        public String getMessage() {
            return msg;
        }

        public String toString() {
            return msg + "(" + completion + ")";
        }
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        String annoType = annotation.getAnnotationType().toString();
        boolean noText = userText == null || userText.trim().isEmpty();
        if (HELP_ANNO.equals(annoType)) {
            if (member != null && member.getSimpleName() != null) {
                switch (member.getSimpleName().toString()) {
                    case "related":
                        userText = stripQuotes(userText);
                        noText = userText == null || userText.trim().isEmpty();
                        Set<Completion> idcs = idCompletions(userText);
                        return idcs;
                    case "content":
                        Locale loc = Locale.getDefault();
                        StringBuilder sb = new StringBuilder("{@Help.HelpText(language=\"");
                        sb.append(loc.getLanguage()).append("\"");
                        if (loc.getCountry() != null && !loc.getCountry().isEmpty()) {
                            sb.append(", country=\"").append(loc.getCountry()).append('"');
                        }
                        // The API here is seriously inadequate to figure out if the
                        // closing parenthesis is already on the annotation
                        sb.append(", value=\"<pending>\")})");
                        return Collections.singleton(new GenericCompletion("New help content for " + loc.getDisplayLanguage() + " / " + loc.getDisplayCountry(), sb.toString()));
                }
            }
        } else if (HELP_TEXT_INNER_ANNO.equals(annoType) || TOPIC_LOC_INNER_ANNO.equals(annoType)) {
            if (member != null && member.getSimpleName() != null) {
                switch (member.getSimpleName().toString()) {
                    case "language":
                        Map<String, String> langForCompletion = new HashMap<>();
                        Set<String> langs = new TreeSet<>();
                        for (String lang : Locale.getISOLanguages()) {
                            if (noText) {
                                langForCompletion.put(lang, lang);
                                langs.add(lang);
                            } else if (lang.startsWith(userText)) {
                                String comp = lang.substring(userText.length(), lang.length());
                                if (!comp.isEmpty()) {
                                    langForCompletion.put(comp, lang);
                                    langs.add(comp);
                                }
                            }
                        }
                        Set<Completion> languageCompletions = new LinkedHashSet<>();
                        if (!langs.isEmpty()) {
                            boolean hadTrailingQuote = userText != null && userText.length() > 1 && userText.charAt(userText.length() - 1) == '"';
                            userText = stripQuotes(userText);
                            noText = userText == null || userText.trim().isEmpty();
                            Locale curr = Locale.getDefault();
                            if (noText || userText.trim().startsWith(curr.getLanguage())) {
                                String comp = curr.getLanguage();
                                boolean defaultLocaleIsMatch;
                                if (!noText && !userText.trim().isEmpty()) {
                                    defaultLocaleIsMatch = comp.startsWith(userText.trim());
                                    if (defaultLocaleIsMatch) {
                                        comp = comp.substring(userText.length(), comp.length());
                                        defaultLocaleIsMatch = !comp.isEmpty();
                                    }
                                } else {
                                    defaultLocaleIsMatch = true;
                                }
                                if (defaultLocaleIsMatch) {
                                    languageCompletions.add(new GenericCompletion(comp, curr.getDisplayLanguage()));
                                    langs.remove(comp);
                                }
                            }
                            for (String lang : langs) {
                                String fullLanguage = langForCompletion.get(lang);
                                Locale loc = Locale.forLanguageTag(fullLanguage);
                                String dispLanguage = loc.getDisplayLanguage();
                                languageCompletions.add(new GenericCompletion(dispLanguage,
                                        lang + (hadTrailingQuote ? "" : '"')));
                            }
                            return languageCompletions;
                        }
                        break;
                    case "country":
                        break;
                }
            }
        }
        return Collections.emptySet();
    }

    private static String stripQuotes(String s) {
        while (s != null && !s.isEmpty() && s.charAt(0) == '"') {
            s = s.substring(1);
        }
        while (s != null && !s.isEmpty() && s.charAt(s.length() - 1) == '"') {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    Set<String> warnedNoTopic = new HashSet<>();

    private void warnNoTopic(TopicInfo forDefiningTypeOrPackage, OneLocaleInfo info) {
        HelpInfo owner = null;
        for (Map.Entry<String, Set<HelpInfo>> e : helpInfosForPackage.entrySet()) {
            for (HelpInfo possibleOwner : e.getValue()) {
                if (possibleOwner.locales.contains(info)) {
                    owner = possibleOwner;
                    break;
                }
            }
        }
        String uid = owner == null ? "null-" : owner.id + "-" + info.languageTag();
        if (warnedNoTopic.contains(uid)) {
            return;
        }
        warnedNoTopic.add(uid);
        if (owner != null) {
            utils.warn("@Topic annotation defined for " + owner.id
                    + " on parent of " + info.originatingElement
                    + ": " + forDefiningTypeOrPackage.target
                    + " but does not define a translation for " + info.languageTag()
                    + ". Using non-localized English default 'General'", info.originatingElement);

        } else {
            utils.warn("@Topic annotation defined on parent of " + info.originatingElement
                    + ": " + forDefiningTypeOrPackage.target
                    + " but does not define a translation for " + info.languageTag()
                    + ". Using non-localized English default 'General'", info.originatingElement);

        }
    }

    String topicFor(OneLocaleInfo info) {
        if (info.topic != null && !info.topic.trim().isEmpty()) {
            return info.topic;
        }
        TopicInfo forDefiningTypeOrPackage = topicForElement(info.originatingElement);
        if (forDefiningTypeOrPackage != null) {
            OneTopicInfo topic = forDefiningTypeOrPackage.forLanguage(info.language, info.country);
            if (topic != null) {
                return topic.topic;
            } else {
                warnNoTopic(forDefiningTypeOrPackage, info);
                return null;

            }
        }
        utils.warn("No @Topic annotation defined on any parent of " + fqnOf(info.originatingElement)
                + " - using non-localized English default 'General'");
        return "General";
    }

    TopicInfo topicForElement(Element el) {
        TopicInfo info = topicForElement.get(typeOrPackageNameOf(el));
        if (info != null) {
            return info;
        }
        Element parent = el.getEnclosingElement();
        if (parent != null) {
            return topicForElement(parent);
        }
        return null;
    }

    static String fqnOf(Element el) {
        String result = typeOrPackageNameOf(el);
        switch (el.getKind()) {
            case CONSTRUCTOR:
                result += ".<init>";
                break;
            case METHOD:
                result += "." + ((ExecutableElement) el).getSimpleName();
                break;
            case ENUM_CONSTANT:
            case FIELD:
            case LOCAL_VARIABLE:
                result += "." + ((VariableElement) el).getSimpleName();
                break;
            case PACKAGE:
            case CLASS:
            case ANNOTATION_TYPE:
            case INTERFACE:
            case ENUM:
                break;
        }
        if (el instanceof ExecutableElement) {
            StringBuilder sb = new StringBuilder(result).append('(');
            ExecutableElement ex = (ExecutableElement) el;
            for (Iterator<? extends VariableElement> it = ex.getParameters().iterator(); it.hasNext();) {
                VariableElement ve = it.next();
                sb.append(ve.asType().toString());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            result = sb.append(')').toString();
        }
        return result;
    }

    static String typeOrPackageNameOf(Element el) {
        while (!(el instanceof TypeElement) && !(el instanceof PackageElement)) {
            el = el.getEnclosingElement();
        }
        if (el == null) {
            return "";
        }
        if (el instanceof PackageElement) {
            return ((PackageElement) el).getQualifiedName().toString();
        }
        return ((TypeElement) el).getQualifiedName().toString();
    }
}
