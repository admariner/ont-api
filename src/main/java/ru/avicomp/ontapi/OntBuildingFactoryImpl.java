package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.jena.OntFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.transforms.Transform;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;

/**
 * Ontology building&loading factory. The 'core', the point to create and load ontologies.
 * See also base interface {@link OWLOntologyFactory} and its single implementation {@link OWLOntologyFactoryImpl}.
 * <p>
 * Created by szuev on 24.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntBuildingFactoryImpl implements OntologyManager.Factory {

    static {
        ErrorHandlerFactory.setDefaultErrorHandler(ErrorHandlerFactory.errorHandlerNoLogging);
    }

    protected final OntBuilderImpl ontologyBuilder;
    protected final OntLoader ontologyLoader;

    public OntBuildingFactoryImpl() {
        ontologyBuilder = new OntBuilderImpl();
        ontologyLoader = new OntModelLoaderImpl(ontologyBuilder);
    }

    @Override
    public boolean canCreateFromDocumentIRI(@Nonnull IRI iri) {
        return true;
    }

    @Override
    public boolean canAttemptLoading(@Nonnull OWLOntologyDocumentSource source) {
        return !source.hasAlredyFailedOnStreams() ||
                !source.hasAlredyFailedOnIRIResolution() &&
                        OntConfig.DefaultScheme.all().anyMatch(s -> s.same(source.getDocumentIRI()));
    }

    @Override
    public OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager,
                                           @Nonnull OWLOntologyID id,
                                           @Nonnull IRI documentIRI,
                                           @Nonnull OWLOntologyCreationHandler handler) {
        return ontologyBuilder.make(asONT(manager), id);
    }

    @Override
    public OntologyModel loadOWLOntology(@Nonnull OWLOntologyManager manager,
                                         @Nonnull OWLOntologyDocumentSource source,
                                         @Nonnull OWLOntologyCreationHandler handler,
                                         @Nonnull OWLOntologyLoaderConfiguration configuration) throws OWLOntologyCreationException {
        return ontologyLoader.load(source, asONT(manager), asONT(configuration));
    }

    /**
     * Currently it is just sugar.
     *
     * @param manager {@link OWLOntologyManager}
     * @return {@link OntologyManager}
     */
    public static OntologyManager asONT(OWLOntologyManager manager) {
        return (OntologyManager) manager;
    }

    /**
     * Wraps {@link OWLOntologyLoaderConfiguration} as {@link OntConfig.LoaderConfiguration}
     *
     * @param conf {@link OWLOntologyLoaderConfiguration}
     * @return {@link ru.avicomp.ontapi.OntConfig.LoaderConfiguration}
     */
    public static OntConfig.LoaderConfiguration asONT(OWLOntologyLoaderConfiguration conf) {
        return conf instanceof OntConfig.LoaderConfiguration ? (OntConfig.LoaderConfiguration) conf : new OntConfig.LoaderConfiguration(conf);
    }

    /**
     * Base class for any model loader.
     * Currently there are two implementations:
     * - pure OWL loader which calls super method of {@link OWLOntologyFactoryImpl}
     * - the {@link OntModelLoaderImpl}.
     * <p>
     * Note: only two input parameters in the constructor: {@link OntologyManager} and {@link OWLOntologyLoaderConfiguration}.
     * The single instance of {@link OntologyManager} is an {@link OWLOntologyManager} as well as {@link OWLOntologyCreationHandler}.
     * And this is also true for {@link uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl).
     * The {@link OWLOntologyCreationHandler} could be considered as part of inner (OWL-API) implementation,
     * so there is no need in this parameter in our case.
     */
    @SuppressWarnings("WeakerAccess")
    public interface OntLoader extends Serializable {

        /**
         * base method to load model ({@link OntologyModel}) to the manager ({@link OntologyManager}).
         * if the result model contains imports they should come as models also.
         *
         * @param source  {@link OWLOntologyDocumentSource} the source (iri, file iri, stream or who knows what)
         * @param manager {@link OntologyManager}
         * @param conf    {@link OntConfig.LoaderConfiguration}
         * @return {@link OntologyModel} the result model in the manager.
         * @throws OWLOntologyCreationException if something wrong.
         */
        OntologyModel load(OWLOntologyDocumentSource source, OntologyManager manager, OntConfig.LoaderConfiguration conf) throws OWLOntologyCreationException;

    }

    /**
     * To load {@link OntologyModel} through pure OWL-API mechanisms (using {@link OWLOntologyFactoryImpl}).
     * Some formats (such as {@link org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat} or {@link org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat}),
     * are not supported by jena, so it is the only way.
     */
    public static class OWLLoaderImpl implements OntLoader {
        protected static final Logger LOGGER = LoggerFactory.getLogger(OWLLoaderImpl.class);

        protected final OWLOntologyFactoryImpl factory;

        // to avoid recursion loop,
        // which may happen since OWL-API parsers may use the manager again, which uses factory with the same parsers
        protected Set<IRI> sources = new HashSet<>();

        public OWLLoaderImpl(OntBuilderImpl builder) {
            factory = new OWLOntologyFactoryImpl(OntApiException.notNull(builder, "Null builder"));
        }

        @Override
        public OntologyModel load(OWLOntologyDocumentSource source, OntologyManager manager, OntConfig.LoaderConfiguration conf) throws OWLOntologyCreationException {
            try {
                IRI doc = source.getDocumentIRI();
                if (sources.contains(doc)) {
                    throw new BadRecursionException("Cycle loading for source " + doc);
                }
                sources.add(doc);
                OntologyModel res = (OntologyModel) factory.loadOWLOntology(manager, source, (OWLOntologyCreationHandler) manager, conf);
                if (LOGGER.isDebugEnabled()) {
                    OntFormat format = OntFormat.get(manager.getOntologyFormat(res));
                    LOGGER.debug("The ontology <{}> is loaded. Format: {}[{}]", res.getOntologyID(), format, source.getClass().getSimpleName());
                }
                // clear cache to be sure that list of axioms is always the same and corresponds to the graph.
                res.clearCache();
                return res;
            } finally {
                sources.clear();
            }
        }
    }

    /**
     * The main {@link OntLoader}.
     * This is an auxiliary class to provide loading of {@link OntologyModel} using jena.
     * Should resolves problems such as cycle imports or throws informative exceptions.
     * In case of some problems while loading there is no need to clear manager to keep it synchronized
     * since models are assembled after obtaining the graphs collection.
     */
    public static class OntModelLoaderImpl implements OntLoader {
        protected static final Logger LOGGER = LoggerFactory.getLogger(OntModelLoaderImpl.class);

        protected Map<String, GraphInfo> graphs = new LinkedHashMap<>();
        protected OntLoader alternative;

        public OntModelLoaderImpl(OntLoader alternative) {
            this.alternative = alternative;
        }

        public OntModelLoaderImpl(OntBuilderImpl builder) {
            this(new OWLLoaderImpl(OntApiException.notNull(builder, "Null builder.")));
        }

        @Override
        public OntologyModel load(OWLOntologyDocumentSource source, OntologyManager manager, OntConfig.LoaderConfiguration config) throws OWLOntologyCreationException {
            try {
                GraphInfo primary;
                try {
                    primary = loadGraph(source, config);
                } catch (UnsupportedFormatException e) {
                    if (alternative == null) {
                        throw new OWLOntologyCreationException("Unable to load graph from " + source, e);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        String cause = e.getCause() != null ? e.getCause().getMessage() : null;
                        LOGGER.debug("Can't load using jena ({}|{}), use original method.", e.getMessage(), cause);
                    }
                    // if we are not success with primary graph there is no reason to continue loading through this loader.
                    try {
                        return alternative.load(source, manager, config);
                    } catch (OWLOntologyCreationException ex) {
                        if (ex.getCause() == null)
                            ex.initCause(e);
                        throw ex;
                    }
                }
                // null key in case of anonymous ontology. But: only one anonymous is allowed (as root of imports tree).
                graphs.put(primary.getURI(), primary);
                // first expand graphs map by creating primary model:
                OntologyModel res = OntApiException.notNull(createModel(primary, manager, config), "Should never happen");
                // then process all the rest dependent models (we have already all graphs compiled, now need populate them as models):
                List<GraphInfo> graphs = this.graphs.keySet().stream()
                        .filter(u -> !Objects.equals(u, primary.getURI()))
                        .map(k -> this.graphs.get(k)).collect(Collectors.toList());
                for (GraphInfo c : graphs) {
                    createModel(c, manager, config);
                }
                return res;
            } finally { // the possibility to reuse.
                graphs.clear();
            }
        }

        /**
         * Populates {@link Graph} as {@link OntologyModel} inside manager.
         *
         * @param info    {@link GraphInfo} container with info about graph.
         * @param manager {@link OntologyManager}
         * @param config  {@link OntConfig.LoaderConfiguration}
         * @return {@link OntologyModel}, it is ready to use.
         * @throws OWLOntologyCreationException if can't assemble model from ready graph.
         */
        protected OntologyModel createModel(GraphInfo info, OntologyManager manager, OntConfig.LoaderConfiguration config) throws OWLOntologyCreationException {
            if (!info.isFresh()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The ontology <{}> is already configured.", info.name());
                }
                return null;
            }
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Set up ontology <{}>.", info.name());
                }
                boolean isPrimary = graphs.size() == 1;
                Graph graph = makeUnionGraph(info, new HashSet<>(), manager, config, isPrimary);
                OntFormat format = info.getFormat();
                InternalModel base = new InternalModel(graph, config.getPersonality());
                base.setLoaderConfig(config);
                base.setWriterConfig(manager.getOntologyWriterConfiguration());
                base.setDataFactory(manager.getOWLDataFactory());
                OntologyModelImpl ont = new OntologyModelImpl(manager, base);
                OntologyModel res = ((OntologyManagerImpl) manager).isConcurrent() ? ont.toConcurrentModel() : ont;
                if (manager.contains(res)) {
                    throw new OWLOntologyAlreadyExistsException(res.getOntologyID());
                }
                ((OntologyManagerImpl) manager).ontologyCreated(res);
                OWLDocumentFormat owlFormat = format.createOwlFormat();
                if (PrefixManager.class.isInstance(owlFormat)) {
                    PrefixManager pm = (PrefixManager) owlFormat;
                    graph.getPrefixMapping().getNsPrefixMap().forEach(pm::setPrefix);
                    OntologyManagerImpl.setDefaultPrefix(pm, ont);
                }
                if (isPrimary) {
                    // todo: should we pass stats from transforms? do we need it?
                    OWLOntologyLoaderMetaData fake = new RDFParserMetaData(RDFOntologyHeaderStatus.PARSED_ONE_HEADER, 0,
                            Collections.emptySet(), ArrayListMultimap.create());
                    owlFormat.setOntologyLoaderMetaData(fake);
                }
                manager.setOntologyFormat(res, owlFormat);
                return res;
            } finally { // just in case.
                info.setProcessed();
            }
        }

        /**
         * Assembles the {@link UnionGraph} from the inner collection ({@link #graphs}).
         *
         * @param node      {@link GraphInfo}
         * @param seen      Collection of URIs to avoid cycles in imports.
         * @param manager   {@link OntologyManager}
         * @param config    {@link OntConfig.LoaderConfiguration}
         * @param transform performs graph-transformation on the specified graph if true.
         * @return {@link Graph}
         * @throws OntApiException if something wrong.
         */
        protected Graph makeUnionGraph(GraphInfo node, Collection<String> seen, OntologyManager manager, OntConfig.LoaderConfiguration config, boolean transform) {
            Set<GraphInfo> children = new HashSet<>();
            Graph main = node.getGraph();
            seen.add(node.getURI());
            List<String> imports = new ArrayList<>(node.getImports());
            for (int i = 0; i < imports.size(); i++) {
                String uri = imports.get(i);
                if (seen.contains(uri)) continue;
                OWLImportsDeclaration declaration = manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(uri));
                if (config.isIgnoredImport(declaration.getIRI())) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("{} is ignored.", declaration);
                    }
                    continue;
                }
                // graphs#computeIfAbsent:
                GraphInfo info = graphs.get(uri);
                try {
                    if (info == null)
                        info = fetchGraph(uri, manager, config);
                    graphs.put(uri, info);
                    // Anonymous ontology or ontology without header (i.e. if no "_:x rdf:type owl:Ontology") could be loaded
                    // if there is some resource-mapping in the manager on the import declaration.
                    // In this case we may load it as separated model or include to the parent graph:
                    if (info.isAnonymous() && MissingOntologyHeaderStrategy.INCLUDE_GRAPH.equals(config.getMissingOntologyHeaderStrategy())) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("<{}>: remove import declaration <{}>.", node.name(), uri);
                        }
                        main.remove(Node.ANY, OWL.imports.asNode(), NodeFactory.createURI(uri));
                        GraphUtil.addInto(main, info.getGraph());
                        graphs.put(uri, info);
                        // skip assembling new model for this graph:
                        info.setProcessed();
                        // recollect imports (in case of anonymous ontology):
                        imports.addAll(i + 1, info.getImports());
                        continue;
                    }
                    children.add(info);
                } catch (OWLOntologyCreationException e) {
                    if (MissingImportHandlingStrategy.THROW_EXCEPTION.equals(config.getMissingImportHandlingStrategy())) {
                        throw new UnloadableImportException(e, declaration);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Can't read sub graph with " + declaration + ". Exception: " + e);
                    }
                }
            }
            UnionGraph res = new UnionGraph(main);
            children.forEach(ch -> res.addGraph(makeUnionGraph(ch, seen, manager, config, transform)));
            return transform ? transform(res, config) : res;
        }

        /**
         * Makes graph transformation if it is allowed in the settings.
         * All sub-graphs should be already transformed.
         *
         * @param graph         {@link Graph}
         * @param configuration {@link OntConfig.LoaderConfiguration}
         * @return {@link Graph}
         * @see Transform
         */
        protected Graph transform(Graph graph, OntConfig.LoaderConfiguration configuration) {
            if (configuration.isPerformTransformation()) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Perform graph transformations.");
                configuration.getGraphTransformers().actions(graph).forEach(Transform::process);
            }
            return graph;
        }

        /**
         * Returns the {@link Graph} wrapped by {@link GraphInfo} which corresponds the specified ontology uri.
         * If there the model ({@link OntologyModel}) with the specified uri already exists inside manager then
         * the method returns the base graph from it.
         * Otherwise it tries to load graph directly by uri or using predefined document iri from
         * some manager's iri mapper (see {@link OWLOntologyIRIMapper}).
         *
         * @param uri     String the ontology uri.
         * @param manager {@link OntologyManager}
         * @param config  {@link OntConfig.LoaderConfiguration}
         * @return {@link GraphInfo} container with {@link Graph} encapsulated.
         * @throws ConfigMismatchException      a conflict with some settings from <code>config</code>
         * @throws OWLOntologyCreationException some serious I/O problem while loading
         * @throws OntApiException              some other unexpected problem occurred.
         */
        protected GraphInfo fetchGraph(String uri, OntologyManager manager, OntConfig.LoaderConfiguration config) throws OWLOntologyCreationException {
            IRI ontologyIRI = IRI.create(uri);
            OntologyModel res = findModel(manager, ontologyIRI);
            if (res != null) {
                return toGraphInfo(res, false);
            }
            IRI documentIRI = Iter.asStream(manager.getIRIMappers().iterator())
                    .map(m -> m.getDocumentIRI(ontologyIRI)).filter(Objects::nonNull)
                    .findFirst().orElse(ontologyIRI);
            // handle also the strange situation when there is no resource-mapping but a mapping on some existing ontology
            res = findModel(manager, documentIRI);
            if (res != null) {
                return toGraphInfo(res, false);
            }
            IRIDocumentSource source = new IRIDocumentSource(documentIRI);
            try {
                return loadGraph(source, config);
            } catch (UnsupportedFormatException e) {
                if (alternative == null) {
                    throw e;
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Can't load graph using jena ({}), try alternative method.", e.getMessage());
                }
                try {
                    // we need only the base graph
                    OntologyManagerImpl m = new OntologyManagerImpl(manager.getOWLDataFactory(), new NoOpReadWriteLock());
                    OntConfig.LoaderConfiguration conf = m.getOntologyLoaderConfiguration()
                            .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
                    return toGraphInfo(alternative.load(source, m, conf), true);
                } catch (OWLOntologyCreationException ex) {
                    if (ex.getCause() == null)
                        ex.initCause(e);
                    throw ex;
                }
            }
        }

        /**
         * Finds ontology by the IRI.
         * <p>
         * WARNING: the jena iri-resolver ({@link org.apache.jena.riot.system.IRIResolver})
         * makes all graphs iri's in one common form according to some inner rule, which I can't change,
         * but which, I believe, corresponds to the <a href='https://tools.ietf.org/html/rfc3986'>URI standard</a>,
         * ... and also to the <a href='http://ietf.org/rfc/rfc3987'>IRI standard</a>.
         * It happens while writing(saving) ontology as Turtle (at least).
         * And it looks like Jena(3.0.1) bug.
         * As a result if we have OWL-API IRI like this 'file:/C:/Users/admin/AppData/Local/Temp/tmp.file'
         * (which may come from expression {@code IRI.create({@link File})})
         * after reloading ontology it would looks like 'file:///C:/Users/admin/AppData/Local/Temp/tmp.file' inside graph.
         * This method is a quick workaround to handle correctly such situations.
         *
         * @param m   {@link OntologyManager}
         * @param iri {@link IRI}
         * @return {@link OntologyModel} or null.
         * @see org.apache.jena.riot.system.IRIResolver
         */
        protected OntologyModel findModel(OntologyManager m, IRI iri) {
            OntologyModel res = m.getOntology(new OWLOntologyID(Optional.of(iri), Optional.empty()));
            if (res != null) return res;
            if (iri.toString().startsWith("file://")) { // hack:
                iri = IRI.create(iri.toString().replaceAll("/+", "/"));
                return m.getOntology(new OWLOntologyID(Optional.of(iri), Optional.empty()));
            }
            return null;
        }

        /**
         * Wraps model as inner container {@link GraphInfo}.
         *
         * @param model {@link OntologyModel}
         * @param fresh if true result should be considered as fresh.
         * @return {@link GraphInfo}
         */
        protected GraphInfo toGraphInfo(OntologyModel model, boolean fresh) { // npe in case no format?
            OntFormat format = OntFormat.get(model.getOWLOntologyManager().getOntologyFormat(model));
            Graph graph = model.asGraphModel().getBaseGraph();
            return new GraphInfo(graph, format, fresh);
        }

        /**
         * Loads graph from the source.
         *
         * @param source {@link OWLOntologyDocumentSource} with instructions how to reach the graph.
         * @param config {@link OntConfig.LoaderConfiguration}
         * @return {@link GraphInfo} wrapper around the {@link Graph}.
         * @throws UnsupportedFormatException   if source can't be read into graph using jena way.
         * @throws ConfigMismatchException      if conflict with some config settings.
         * @throws OWLOntologyCreationException if there is some serious I/O problem.
         * @throws OntApiException              if some other problem occurred.
         */
        protected GraphInfo loadGraph(OWLOntologyDocumentSource source, OntConfig.LoaderConfiguration config) throws OWLOntologyCreationException {
            Graph graph;
            OntFormat format;
            if (OntGraphDocumentSource.class.isInstance(source)) {
                OntGraphDocumentSource _source = (OntGraphDocumentSource) source;
                graph = _source.getGraph();
                format = _source.getOntFormat();
            } else {
                graph = OntFactory.createDefaultGraph();
                format = readGraph(graph, source, config);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Graph <{}> is loaded. Source: {}[{}]. Format: {}",
                        Graphs.getName(graph), source.getClass().getSimpleName(), source.getDocumentIRI(), format);
            }
            return new GraphInfo(graph, format, true);
        }

        /**
         * The main method to read the source document to the graph.
         * For generality it is public.
         *
         * @param graph  {@link Graph} the graph(empty) to put in.
         * @param source {@link OWLOntologyDocumentSource} the source (encapsulates IO-stream, IO-Reader or IRI of document)
         * @param conf   {@link OntConfig.LoaderConfiguration} config
         * @return {@link OntFormat} corresponding to the specified source.
         * @throws UnsupportedFormatException   if source can't be read into graph using jena.
         * @throws ConfigMismatchException      if there is some conflict with config settings, anyway we can't continue.
         * @throws OWLOntologyCreationException if there is some serious IO problem
         * @throws OntApiException              if some other problem.
         */
        public static OntFormat readGraph(Graph graph, OWLOntologyDocumentSource source, OntConfig.LoaderConfiguration conf) throws OWLOntologyCreationException {
            IRI iri = OntApiException.notNull(source, "Null document source.").getDocumentIRI();
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Read graph from <{}>.", iri);
            if (source.getInputStream().isPresent()) {
                return read(graph, source, s -> s.getInputStream().get());
            }
            if (source.getReader().isPresent()) {
                return read(graph, source, s -> buffer(asInputStream(s.getReader().get())));
            }
            if (conf.getSupportedSchemes().stream().noneMatch(s -> s.same(iri))) {
                throw new ConfigMismatchException("Not allowed scheme: " + iri);
            }
            return read(graph, source, s -> DocumentSources.getInputStream(iri, conf)
                    .orElseThrow(() -> new OWLOntologyInputSourceException("Can't get input stream from " + iri)));
        }

        /**
         * Tries to compute the {@link OntFormat} from {@link OWLOntologyDocumentSource} by using content type or uri or whatever else,
         * but not encapsulated OWL-format (which may absent).
         * public, for more generality.
         *
         * @param source {@link OWLOntologyDocumentSource}
         * @return {@link OntFormat} or null if it could not guess format from source.
         */
        public static OntFormat guessFormat(OWLOntologyDocumentSource source) {
            Lang lang;
            if (OntApiException.notNull(source, "Null document source.").getMIMEType().isPresent()) {
                lang = RDFLanguages.contentTypeToLang(source.getMIMEType().get());
            } else {
                lang = RDFLanguages.filenameToLang(source.getDocumentIRI().getIRIString());
            }
            return lang == null ? null : OntFormat.get(lang);
        }

        /**
         * Returns supported formats related to the source.
         * The result (ordered set) can only contain a single format in case the source has {@link OWLDocumentFormat},
         * otherwise it will contain all supported formats.
         *
         * @param source {@link OWLOntologyDocumentSource}
         * @return Set of {@link OntFormat}s
         */
        public static Set<OntFormat> getSupportedFormats(OWLOntologyDocumentSource source) {
            Set<OntFormat> res = new LinkedHashSet<>();
            if (source.getFormat().isPresent()) {
                OntFormat f = OntFormat.get(source.getFormat().get());
                if (f.isSupported()) {
                    res.add(f);
                    return res;
                }
            }
            OntFormat first = guessFormat(source);
            if (first != null) {
                res.add(first);
            }
            OntFormat.supported().forEach(res::add);
            return res;
        }

        /**
         * Performs reading to the graph from the source using ont-supplier which produces input stream each time
         *
         * @param graph    {@link Graph}
         * @param source   {@link OWLOntologyDocumentSource}
         * @param supplier {@link OntInputSupplier}
         * @return {@link OntFormat}
         * @throws OWLOntologyCreationException if smth wrong.
         */
        protected static OntFormat read(Graph graph, OWLOntologyDocumentSource source, OntInputSupplier supplier) throws OWLOntologyCreationException {
            IRI iri = source.getDocumentIRI();
            OWLOntologyCreationException cause = null;
            for (OntFormat format : getSupportedFormats(source)) {
                if (format.isOWLOnly()) {
                    if (cause == null) {
                        cause = new UnsupportedFormatException("Format " + format + " is not supported by jena.");
                    }
                    continue;
                }
                Lang lang = format.getLang();
                try (InputStream is = supplier.open(source)) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("try <{}>", lang);
                    RDFDataMgr.read(graph, is, lang);
                    return format;
                } catch (OWLOntologyInputSourceException | IOException e) {
                    throw new OWLOntologyCreationException("Can't open or close input stream from " + iri, e);
                } catch (RuntimeException e) {
                    // could be org.apache.jena.shared.JenaException || org.apache.jena.atlas.AtlasException || org.apache.jena.atlas.json.JsonParseException || ...
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("<{}> failed: '{}'", lang, e.getMessage());
                    cause = new UnsupportedFormatException(String.format("Can't read %s from iri <%s>: %s", format, iri, e.getMessage()), e);
                }
            }
            if (cause == null)
                cause = new UnsupportedFormatException(String.format("Can't read from source %s[%s].", iri, source));
            throw cause;
        }

        protected static InputStream asInputStream(Reader reader) {
            return new ReaderInputStream(reader, StandardCharsets.UTF_8);
        }

        protected static InputStream buffer(InputStream is) {
            return new BufferedInputStream(is);
        }

        protected interface OntInputSupplier {

            /**
             * NOTE: the caller is responsible for ensuring that the returned stream is closed.
             *
             * @param source {@link OWLOntologyDocumentSource}
             * @return {@link InputStream}
             * @throws OWLOntologyInputSourceException in case something wrong.
             */
            InputStream open(OWLOntologyDocumentSource source) throws OWLOntologyInputSourceException;
        }

        /**
         * Just container for {@link Graph} and {@link OntFormat}.
         * Used for simplification as temporary storage.
         */
        protected class GraphInfo {
            private final OntFormat format;
            private final Graph graph;
            private boolean fresh;
            private String uri;
            private Set<String> imports;

            protected GraphInfo(Graph graph, OntFormat format, boolean fresh) {
                this.graph = graph;
                this.format = format;
                this.fresh = fresh;
            }

            protected String getURI() {
                return uri == null ? uri = Graphs.getURI(graph) : uri;
            }

            protected boolean isAnonymous() {
                return getURI() == null;
            }

            protected String name() {
                return Graphs.getName(graph);
            }

            protected Set<String> getImports() {
                return imports == null ? imports = Graphs.getImports(graph) : imports;
            }

            protected boolean isFresh() {
                return fresh;
            }

            protected void setProcessed() {
                this.fresh = false;
            }

            protected OntFormat getFormat() {
                return format;
            }

            protected Graph getGraph() {
                return graph;
            }
        }
    }

    public static class OntBuilderImpl implements OWLOntologyBuilder {

        @Override
        public OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager, @Nonnull OWLOntologyID id) {
            OntologyManagerImpl m = (OntologyManagerImpl) manager;
            OntologyModelImpl ont = new OntologyModelImpl(m, id);
            return m.isConcurrent() ? ont.toConcurrentModel() : ont;
        }

        /**
         * Creates fresh ontology (ONT) inside manager.
         * I hate RDF/XML. The default format is Turtle (it is difference with OWL-API).
         *
         * @param manager {@link OntologyManager}
         * @param id      {@link OWLOntologyID}
         * @return {@link OntologyModel}
         * @see OntFormat#TURTLE
         */
        public OntologyModel make(@Nonnull OntologyManager manager, @Nonnull OWLOntologyID id) {
            OntologyModel res = createOWLOntology(manager, id);
            ((OntologyManagerImpl) manager).ontologyCreated(res);
            manager.setOntologyFormat(res, OntFormat.TURTLE.createOwlFormat());
            return res;
        }
    }

    public static class ConfigMismatchException extends OWLOntologyCreationException {
        public ConfigMismatchException(String s) {
            super(s);
        }
    }

    public static class UnsupportedFormatException extends OWLOntologyCreationException {

        public UnsupportedFormatException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnsupportedFormatException(String message) {
            super(message);
        }
    }

    public static class BadRecursionException extends OWLOntologyCreationException {

        public BadRecursionException(String message) {
            super(message);
        }
    }

}
