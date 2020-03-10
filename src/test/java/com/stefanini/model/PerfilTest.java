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

public class PerfilTest {
    private Validator validator;
    private SessionFactory factoryJpa;
    private Boolean h2Carregador = Boolean.FALSE;

    private  String nome = "ADMIN";

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
   
    private Perfil findPerfilUsandoNameQuery(Session session, String name) {
        TypedQuery<Perfil> q2 =
                session.createNamedQuery("Perfil.findPerfilByNome", Perfil.class);
        q2.setParameter("nome", name);
        Perfil perfil = q2.getSingleResult();
        return perfil;
    }
    
    @Test
    public void findPerfilUsandoNameQuery() {
        try (Session session = factoryJpa.openSession()) {
            Perfil perfil = findPerfilUsandoNameQuery(session, nome);
            System.out.println("Nome :" +perfil.getNome());
        }
    }
    
    private Perfil findPerfilNovo(Session session, String name) {
        TypedQuery<Perfil> q2 =
                session.createQuery("select p from Perfil p where p.nome=:nome", Perfil.class);
        q2.setParameter("nome", name);
        Perfil perfil = q2.getSingleResult();
        return perfil;
    }
    
    /**
     * EFETUAR A  COM TYPEDQUERY
     * QUANDO NÃO POSSUI LANCA UMA NoResultException
     */
    @Test
    public void perfilComTypedQuery() {
        try (Session session = factoryJpa.openSession()) {
            Perfil perfil = findPerfilNovo(session, nome);
            System.out.println("Novo: " + perfil);
        }
    }
    
    private Perfil findPerfilAntigo(Session session, String name) {
        Query<Perfil> query = session.createQuery("select p from Perfil p where p.nome=:nome");
        query.setParameter("nome", name);
        Perfil perfil = query.uniqueResult();
        return perfil;
    }
    
    @Test
    public void PerfilComQuery() {
        try (Session session = factoryJpa.openSession()) {
            Perfil perfil = findPerfilAntigo(session, nome);
            System.out.println("Antigo: " + perfil);
        }
    }
    
    /**
     * JPA 2.2
     *
     * @param session
     * @param name
     * @return
     */
    private Stream<Perfil> findPerfilUsandoNameQueryComStream(Session session, String name) {
        TypedQuery<Perfil> q2 =
                session.createQuery(" select p from Perfil p where p.nome=:nome", Perfil.class);
        q2.setParameter("nome", name);
        return q2.getResultStream();
    }
    
    /**
     * EFETUAR A  COM NameQuery
     */
    @Test()
    public void findPessoaStream() {
        try (Session session = factoryJpa.openSession()) {
            Stream<Perfil> perfilUsandoNameQueryComStream = findPerfilUsandoNameQueryComStream(session, nome);
            perfilUsandoNameQueryComStream.forEach(System.out::println);
        }
    }
    
    /* Evitar Erros de sintaxe
    *
    * @param session
    * @param name
    * @return
    */
   private Long countPerfilCriteria(Session session, String name) {
       CriteriaBuilder cb = session.getCriteriaBuilder();
       CriteriaQuery<Long> q = cb.createQuery(Long.class);
       Root<Perfil> entityRoot = q.from(Perfil.class);
       q.select(cb.count(entityRoot));
       ParameterExpression<String> p = cb.parameter(String.class);
       q.where(cb.equal(entityRoot.get("nome"), name));
       System.out.println("QUERY : " + session.createQuery(q).getQueryString());
       return session.createQuery(q).getSingleResult();
   }
    /**
     * EFETUAR A  COM NameQuery
     */
    @Test()
    public void findCountPerfil() {
        try (Session session = factoryJpa.openSession()) {
            Long qtd = countPerfilCriteria(session, nome);
            System.out.println("QTD: " + qtd);
        }
    }
}
