package com.stefanini.model;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Stream;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.h2.tools.RunScript;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;

public class EnderecoTest {
    private Validator validator;
    private SessionFactory factoryJpa;
    private Boolean h2Carregador = Boolean.FALSE;

    private  String uf = "DF";

    @Before
    public void setUp() {
        runScrip();
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validator = validatorFactory.getValidator();
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure()
                .build();
        factoryJpa = new MetadataSources(registry).buildMetadata().buildSessionFactory();

    }

    public void runScrip() {
        Connection conn = null;
        try {
            conn = DriverManager.
                    getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");
            if (conn != null) {
                final Statement st = conn.createStatement();
                final ResultSet rs = st.executeQuery("show tables");
                while (rs.next()) {
                    h2Carregador = true;
                }
                if (!h2Carregador) {
                    ClassLoader classLoader = getClass().getClassLoader();
                    File file = new File(classLoader.getResource("db.sql").getFile());
                    System.out.println("Carregado o SCRIPT");
                    RunScript.execute(conn, new FileReader(file));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Endereco findEnderecolUsandoNameQuery(Session session, String uf) {
        TypedQuery<Endereco> q2 =
                session.createNamedQuery("Endereco.findEnderecoByUf", Endereco.class);
        q2.setParameter("uf", uf);
        Endereco endereco = q2.getSingleResult();
        return endereco;
    }
    
    @Test
    public void findPerfilUsandoNameQuery() {
        try (Session session = factoryJpa.openSession()) {
        	Endereco end = findEnderecolUsandoNameQuery(session, uf);
            System.out.println("UF :" +end.getUf());
        }
    }
    
    private Endereco findEnderecoNovo(Session session, String uf) {
        TypedQuery<Endereco> q2 =
                session.createQuery("select e from Endereco e where e.uf=:uf", Endereco.class);
        q2.setParameter("uf", uf);
        Endereco endereco = q2.getSingleResult();
        return endereco;
    }
    
    /**
     * EFETUAR A  COM TYPEDQUERY
     * QUANDO NÃO POSSUI LANCA UMA NoResultException
     */
    @Test
    public void perfilComTypedQuery() {
        try (Session session = factoryJpa.openSession()) {
        	Endereco endereco = findEnderecoNovo(session, uf);
            System.out.println("Novo: " + endereco);
        }
    }
    
    private Endereco findEnderecoAntigo(Session session, String uf) {
        Query<Endereco> query = session.createQuery("select e from Endereco e where e.uf=:uf");
        query.setParameter("uf", uf);
        Endereco endereco = query.uniqueResult();
        return endereco;
    }
    
    @Test
    public void PerfilComQuery() {
        try (Session session = factoryJpa.openSession()) {
        	Endereco endereco = findEnderecoAntigo(session, uf);
            System.out.println("Antigo: " + endereco);
        }
    }
    
    /**
     * JPA 2.2
     *
     * @param session
     * @param name
     * @return
     */
    private Stream<Endereco> findEnderecoUsandoNameQueryComStream(Session session, String uf) {
        TypedQuery<Endereco> q2 =
                session.createQuery(" select e from Endereco e where e.uf=:uf", Endereco.class);
        q2.setParameter("uf", uf);
        return q2.getResultStream();
    }
    
    /**
     * EFETUAR A  COM NameQuery
     */
    @Test()
    public void findPessoaStream() {
        try (Session session = factoryJpa.openSession()) {
            Stream<Endereco> endUsandoNameQueryComStream = findEnderecoUsandoNameQueryComStream(session, uf);
            endUsandoNameQueryComStream.forEach(System.out::println);
        }
    }
    
    /* Evitar Erros de sintaxe
    *
    * @param session
    * @param uf
    * @return
    */
   private Long countEndCriteria(Session session, String uf) {
       CriteriaBuilder cb = session.getCriteriaBuilder();
       CriteriaQuery<Long> q = cb.createQuery(Long.class);
       Root<Endereco> entityRoot = q.from(Endereco.class);
       q.select(cb.count(entityRoot));
       ParameterExpression<String> p = cb.parameter(String.class);
       q.where(cb.equal(entityRoot.get("uf"), uf));
       System.out.println("QUERY : " + session.createQuery(q).getQueryString());
       return session.createQuery(q).getSingleResult();
   }
    /**
     * EFETUAR A  COM NameQuery
     */
    @Test
    public void findCountPerfil() {
        try (Session session = factoryJpa.openSession()) {
            Long qtd = countEndCriteria(session, uf);
            System.out.println("QTD: " + qtd);
        }
    }
}
