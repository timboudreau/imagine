/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.help.annotation.processors;

import com.mastfrog.annotation.AnnotationUtils;
import com.mastfrog.java.vogon.ClassBuilder;
import com.mastfrog.util.collections.CollectionUtils;
import static com.mastfrog.util.collections.CollectionUtils.setOf;
import java.io.IOException;
import java.io.OutputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import static org.imagine.help.annotation.processors.HelpAnnotationProcessor.HELP_ANNO;
import org.imagine.help.annotation.processors.HelpAnnotationProcessor.HelpInfo.OneLocaleInfo;
import static org.imagine.help.annotation.processors.HelpAnnotationProcessor.TOPIC_ANNO;
import org.imagine.help.annotation.processors.HelpAnnotationProcessor.TopicInfo.OneTopicInfo;
import org.imagine.markdown.uiapi.ErrorConsumer;
import org.imagine.markdown.uiapi.Markdown;
import org.openide.util.lookup.ServiceProvider;

/**
 * Generates an enum constant for each &#064;Help annotation found, an an enum
 * called HelpItems in the same package as the annotated class.
 *
 * @author Tim Boudreau
 */
@SupportedAnnotationTypes({HELP_ANNO, TOPIC_ANNO})
@SupportedOptions(AnnotationUtils.AU_LOG)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@ServiceProvider(service = Processor.class)
public class HelpAnnotationProcessor extends AbstractProcessor {

    public static final String HELP_ANNO = "org.imagine.help.api.annotations.Help";
    public static final String TOPIC_ANNO = "org.imagine.help.api.annotations.Topic";
    private AnnotationUtils utils;
    private BiPredicate<? super AnnotationMirror,? super Element> test;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        utils = new AnnotationUtils(processingEnv, setOf(HELP_ANNO), HelpAnnotationProcessor.class);
        super.init(processingEnv);
        test = utils.multiAnnotations().whereAnnotationType(HELP_ANNO, b -> {
            b.testMember("id").stringValueMustBeValidJavaIdentifier().build()
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
                                    utils.warn(msg);
                                }
                            });
                            return true;
                        }).build();
                    }).build();
        }).whereAnnotationType(TOPIC_ANNO, b -> {
            b.testMemberAsAnnotation("value", amtb -> {
                System.out.println("test one topic anno " + amtb);
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
        System.out.println("Hello world");
        try {
            for (Element el : utils.findAnnotatedElements(roundEnv, HELP_ANNO, TOPIC_ANNO)) {
                Set<AnnotationMirror> mirrors = utils.findAnnotationMirrors(el, HELP_ANNO, TOPIC_ANNO);
                for (AnnotationMirror mir : mirrors) {
                    System.out.println("MIR " + mir);
                    if (test.test(mir, el)) {
                        if (mir.getAnnotationType().toString().equals(HELP_ANNO)) {
                            handleHelpAnnotation(el, mir, roundEnv);
                        } else if (mir.getAnnotationType().toString().equals(TOPIC_ANNO)) {
                            System.out.println("HANDLE TOPIC: " + mir);
                            handleTopicAnnotation(el, mir, roundEnv);
                        }
                    }
                }
            }
            if (roundEnv.processingOver() && !roundEnv.errorRaised()) {
                generate();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            utils.fail(ex.toString());
        }
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
        System.out.println("Handle topic anno " + mir);
        for (AnnotationMirror am : items) {
            String language = utils.annotationValue(am, "language", String.class, "en");
            String country = utils.annotationValue(am, "country", String.class, "en".equals(language) ? "US" : "");
            String text = utils.annotationValue(am, "value", String.class);
            System.out.println("  add " + language + "-" + country + ": '" + text + "'");
            info.add(language, country, text);
        }
    }

    private void handleHelpAnnotation(Element el, AnnotationMirror mir, RoundEnvironment roundEnv) {

        List<AnnotationMirror> texts = utils.annotationValues(mir, "content", AnnotationMirror.class);
        String id = utils.annotationValue(mir, "id", String.class);
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
            handleOneHelpLocale(el, mir, roundEnv, id, language, country, text, topic, keywords);
        }
    }

    private final Map<String, Set<HelpInfo>> toGenerate = CollectionUtils.supplierMap(TreeSet::new);

    private void handleOneHelpLocale(Element el, AnnotationMirror mir, RoundEnvironment roundEnv, String id, String locale, String variant, String text, String topic, List<String> keywords) {
        String packageName = utils.packageName(el);
        HelpInfo info = getOrCreateHelpInfo(packageName, id);
        OneLocaleInfo one = info.add(el, locale, variant, text, topic, keywords);
        if (one == null) {
            utils.fail("Help '" + id + "' for " + locale + (variant.isEmpty() ? "" : "-" + variant)
                    + " is present more than once in package " + packageName, el, mir);
        }
    }

    private HelpInfo getOrCreateHelpInfo(String packageName, String id) {
        Set<HelpInfo> forPackage = toGenerate.get(packageName);
        for (HelpInfo info : forPackage) {
            if (id.equals(info.id)) {
                return info;
            }
        }
        HelpInfo info = new HelpInfo(id);
        forPackage.add(info);
        return info;
    }

    private void generate() {
        if (!toGenerate.isEmpty()) {
            for (Map.Entry<String, Set<HelpInfo>> e : toGenerate.entrySet()) {
                String pkg = e.getKey();
                generateOnePackage(pkg, e.getValue());
            }
            toGenerate.clear();
        }
    }

    private void generateOnePackage(String pkg, Set<HelpInfo> infos) {
        if (infos.isEmpty()) {
            return;
        }
        boolean hasContent = false;
        Set<Element> allElements = new HashSet<>();
        for (HelpInfo ifo : infos) {
            if (!ifo.locales.isEmpty()) {
                hasContent = true;
            }
            allElements.addAll(ifo.elements);
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
                            for (HelpInfo ifo : infos) {
                                idSwitch.inCase(ifo.id, idBlock -> {
                                    idBlock.switchingOn(VAR_LANGUAGE, languageSwitch -> {
                                        ifo.visitLanguagesAndInfos((lang, countries) -> {
                                            languageSwitch.inStringLiteralCase(lang, langCase -> {
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
                                    });
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
                                bb.switchingOn("this", idSwitch -> {
                                    for (HelpInfo ifo : infos) {
                                        idSwitch.inCase(ifo.id, idBlock -> {
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
                                        });
                                    }
                                });
                                bb.returningNull();
                            });
                });
        ClassBuilder<String> helpIndexImpl = ClassBuilder.forPackage(pkg).named("HIndex")
                .extending("AbstractHelpIndex<" + generatedClassName + ">")
                .importing(generatedFqn,
                        "org.imagine.help.api.search.AbstractHelpIndex",
                        "org.openide.util.lookup.ServiceProvider",
                        "org.imagine.help.api.search.HelpIndex",
                        "java.util.Locale"
                )
                .withModifier(Modifier.FINAL, Modifier.PUBLIC)
                .annotatedWith("ServiceProvider", ab -> {
                    ab.addClassArgument("service", "HelpIndex");
                })
                .constructor(con -> {
                    con.setModifier(Modifier.PUBLIC)
                            .body(cbody -> {
                                cbody.invoke("super").withArgument(generatedClassName + ".class")
                                        .inScope();
                            });
                })
                .protectedMethod("init").body(bb -> {
            Set<String> added = new HashSet<>(5);
            for (HelpInfo info : infos) {
                info.visitLanguagesAndInfos((lang, oneLocales) -> {
                    oneLocales.forEach(oneLocale -> {
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
            for (HelpInfo help : infos) {
                // add(Locale language, String topic, String constantName, String[] keywords)
                for (OneLocaleInfo loc : help) {
                    ClassBuilder.InvocationBuilder<?> addInvocation = bb.invoke("add").withArgument(loc.localeVariableName())
                            .withStringLiteral(topicFor(loc)).withStringLiteral(help.id);
                    for (String kwd : loc.keywords()) {
                        addInvocation.withStringLiteral(kwd);
                    }
                    addInvocation.inScope();
                }
            }
        });
        Filer filer = processingEnv.getFiler();
        try {
            JavaFileObject src = filer.createSourceFile(generatedFqn, allElements.toArray(new Element[allElements.size()]));
            try (OutputStream out = src.openOutputStream()) {
                out.write(cb.build().getBytes(UTF_8));
            }
            src = filer.createSourceFile(helpIndexImpl.fqn(), allElements.toArray(new Element[allElements.size()]));
            try (OutputStream out = src.openOutputStream()) {
                out.write(helpIndexImpl.build().getBytes(UTF_8));
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
                utils.warn("@Topic annotation defined on parent of " + info.originatingElement
                        + ": " + forDefiningTypeOrPackage.target
                        + " but does not define a translation for " + info.languageTag()
                        + ". Using non-localized English default 'General'");
                return null;

            }
        }
        utils.warn("No @Topic annotation defined on any parent of " + info.originatingElement
                + " - using non-localized English default 'General'");
        return "General";
    }

    static class TopicInfo implements Comparable<TopicInfo>, Iterable<OneTopicInfo> {

        private final Element target;
        private final Set<OneTopicInfo> entries = new TreeSet<>();

        public TopicInfo(Element target) {
            this.target = target;
        }

        public String toString() {
            switch (target.getKind()) {
                case PACKAGE:
                    PackageElement pe = (PackageElement) target;
                    return pe.getQualifiedName().toString();
                case CLASS:
                    TypeElement te = (TypeElement) target;
                    return te.getQualifiedName().toString();
                default:
                    throw new AssertionError("Unsupported kind " + target.getKind());
            }
        }

        @Override
        public int compareTo(TopicInfo o) {
            return toString().compareTo(o.toString());
        }

        public boolean add(String language, String country, String topic) {
            return entries.add(new OneTopicInfo(language, country, topic));
        }

        public OneTopicInfo forLanguage(String language, String country) {
            for (OneTopicInfo info : this) {
                if (language.equals(info.language) && country.equals(info.country)) {
                    return info;
                }
            }
            for (OneTopicInfo info : this) {
                if (language.equals(info.language)) {
                    return info;
                }
            }
            return null;
        }

        @Override
        public Iterator<OneTopicInfo> iterator() {
            return entries.iterator();
        }

        static class OneTopicInfo implements Comparable<OneTopicInfo> {

            final String language;
            final String country;
            final String topic;

            public OneTopicInfo(String language, String country, String topic) {
                this.language = language;
                this.country = country;
                this.topic = topic;
            }

            public String localeVariableName() {
                if (country == null || country.isEmpty()) {
                    return language.toLowerCase();
                }
                return language.toLowerCase() + "_" + country;
            }

            public String languageTag() {
                if (country == null || country.isEmpty()) {
                    return language;
                }
                return language + "-" + country;
            }

            @Override
            public int compareTo(OneTopicInfo o) {
                return languageTag().compareTo(o.languageTag());
            }

            @Override
            public int hashCode() {
                int hash = 5;
                hash = 79 * hash + Objects.hashCode(this.language);
                hash = 79 * hash + Objects.hashCode(this.country);
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final OneTopicInfo other = (OneTopicInfo) obj;
                if (!Objects.equals(this.language, other.language)) {
                    return false;
                }
                if (!Objects.equals(this.country, other.country)) {
                    return false;
                }
                return true;
            }
        }
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

    private String typeOrPackageNameOf(Element el) {
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

    // Make these sortable, so generation order is consistent for repeatable builds
    static class HelpInfo implements Comparable<HelpInfo>, Iterable<OneLocaleInfo> {

        final String id;
        final List<OneLocaleInfo> locales = new ArrayList<>();
        final Map<String, Set<OneLocaleInfo>> infosByLanguage = CollectionUtils.supplierMap(TreeSet::new);
        final Set<Element> elements = new HashSet<>();

        public HelpInfo(String id) {
            this.id = id;
        }

        public Iterator<OneLocaleInfo> iterator() {
            Collections.sort(locales);
            return locales.iterator();
        }

        Element[] originatingElements() {
            return elements.toArray(new Element[elements.size()]);
        }

        void visitLanguagesAndInfos(BiConsumer<String, Set<OneLocaleInfo>> c) {
            infosByLanguage.forEach((lang, locs) -> {
                if (!locs.isEmpty()) { // can happen with SupplierMap in theory
                    c.accept(lang, locs);
                }
            });
        }

        public OneLocaleInfo fallback() {
            for (OneLocaleInfo ifo : locales) {
                if ("en".equals(ifo.language) && "US".equals(ifo.country)) {
                    return ifo;
                }
            }
            for (OneLocaleInfo ifo : locales) {
                if ("en".equals(ifo.language)) {
                    return ifo;
                }
            }
            for (OneLocaleInfo ifo : locales) {
                if ("".equals(ifo.language)) {
                    return ifo;
                }
            }
            return null;
        }

        OneLocaleInfo add(Element element, String locale, String variant, String text, String topic, List<String> keywords) {
            for (OneLocaleInfo info : locales) {
                if (info.language.equals(locale) && info.country.equals(variant)) {
                    return null;
                }
            }
            OneLocaleInfo result = new OneLocaleInfo(element, locale, variant, text, topic, keywords);
            locales.add(result);
            infosByLanguage.get(locale).add(result);
            return result;
        }

        @Override
        public int compareTo(HelpInfo o) {
            return id.compareTo(o.id);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final HelpInfo other = (HelpInfo) obj;
            return Objects.equals(this.id, other.id);
        }

        static class OneLocaleInfo implements Comparable<OneLocaleInfo> {

            final String language;
            final String country;
            final String text;
            final String topic;
            final List<String> keywords;
            final Element originatingElement;

            public OneLocaleInfo(Element originatingElement, String locale, String country, String text, String topic, List<String> keywords) {
                this.language = locale;
                this.country = country;
                this.text = text;
                this.topic = topic;
                this.keywords = keywords;
                this.originatingElement = originatingElement;
            }

            public String topic() {
                return topic;
            }

            public Set<String> keywords() {
                Set<String> result = new TreeSet<>();
                for (String kwd : keywords) {
                    result.add(kwd.toLowerCase());
                }
                return result;
            }

            public String localeVariableName() {
                if (country == null || country.isEmpty()) {
                    return language.toLowerCase();
                }
                return language.toLowerCase() + "_" + country;
            }

            public String languageTag() {
                if (country == null || country.isEmpty()) {
                    return language;
                }
                return language + "-" + country;
            }

            @Override
            public int compareTo(OneLocaleInfo o) {
                return languageTag().compareTo(o.languageTag());
            }
        }
    }
}
