package com.jobportal.config;

import com.jobportal.model.JobPosting;
import com.jobportal.model.JobStatus;
import com.jobportal.model.Role;
import com.jobportal.model.UserAccount;
import com.jobportal.model.WorkMode;
import com.jobportal.repository.JobRepository;
import com.jobportal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Configuration
public class DemoDataSeeder {
    @Bean
    CommandLineRunner seedDemoJobs(
            JobRepository jobRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed-demo-data:true}") boolean seedDemoData) {
        return args -> {
            if (!seedDemoData) return;

            UserAccount northstar = findOrCreateEmployer(
                    userRepository, passwordEncoder, "hiring@northstar.example",
                    "Northstar Hiring", "Northstar Technologies");
            UserAccount greener = findOrCreateEmployer(
                    userRepository, passwordEncoder, "careers@greener.example",
                    "Greener Hiring", "Greener Goods India");
            localizeLegacyPostings(jobRepository, northstar, greener);
            localizeLegacyInternship(jobRepository, northstar);

            List<JobPosting> postings = List.of(
                    job("Senior Product Designer", "Bengaluru, Karnataka · Hybrid", "Full-time",
                            "Design intuitive products for customers across India. Partner with product and engineering teams to take ideas from user research through launch.",
                            "5+ years of product design experience; strong prototyping skills; portfolio of shipped digital products; ability to collaborate with cross-functional teams.",
                            "18.00", "28.00", northstar),
                    job("Java Spring Boot Developer", "Hyderabad, Telangana", "Full-time",
                            "Build reliable backend services for an Indian technology platform. Work with a collaborative engineering team on REST APIs, PostgreSQL data models, testing, and cloud deployments.",
                            "3+ years of Java and Spring Boot experience; REST APIs, SQL, and Git; experience with unit testing; cloud experience is an advantage.",
                            "12.00", "22.00", northstar),
                    job("Frontend React Developer", "Pune, Maharashtra · Hybrid", "Full-time",
                            "Create accessible, responsive web experiences for customers across India. Collaborate closely with product design and backend engineering.",
                            "2+ years with React and JavaScript; HTML, CSS, responsive design, API integration, and browser testing.",
                            "8.00", "15.00", northstar),
                    job("Digital Marketing Associate", "Mumbai, Maharashtra", "Full-time",
                            "Grow the reach of an Indian sustainable-goods brand. Plan digital campaigns, create clear English-language content, and learn from customer insights.",
                            "2+ years of digital marketing experience; familiarity with SEO, social platforms, and campaign analytics; strong writing skills.",
                            "5.00", "9.00", greener),
                    job("Operations Coordinator", "Delhi NCR · Hybrid", "Full-time",
                            "Help a growing India team run smoothly. Coordinate day-to-day operations, maintain clear process documentation, and work with partners across the country.",
                            "Strong organization and communication; comfortable with spreadsheets; professional fluency in English. Hindi is an advantage.",
                            "4.50", "7.00", greener),
                    internship("Software Engineering Intern", "Chennai, Tamil Nadu · On-site",
                            "Learn with an experienced engineering team and contribute to real product work. This structured internship includes mentorship, code reviews, and a scoped project.",
                            "Currently pursuing a degree in computer science or a related field; programming fundamentals; curiosity and willingness to learn. Internship is intended for students and is not a permanent employee position.",
                            "20000", "35000", northstar),
                    job("Data Analyst", "Gurugram, Haryana · Hybrid", "Full-time",
                            "Turn business and customer data into clear insights for teams building digital services across India. Own dashboards, investigate trends, and present recommendations to stakeholders.",
                            "2+ years in analytics; strong SQL and spreadsheet skills; experience with a BI tool such as Power BI or Tableau; clear communication.",
                            "7.00", "13.00", northstar),
                    job("Product Manager", "Mumbai, Maharashtra · Hybrid", "Full-time",
                            "Shape the roadmap for a customer-facing financial technology product. Partner with design, engineering, operations, and compliance to deliver reliable experiences.",
                            "4+ years in product management; experience writing product requirements, analyzing metrics, and working with cross-functional teams.",
                            "18.00", "30.00", northstar),
                    job("Talent Acquisition Specialist", "Bengaluru, Karnataka", "Full-time",
                            "Help a growing Indian technology team hire thoughtfully. Partner with hiring managers, build candidate pipelines, and create a positive interview experience.",
                            "3+ years of recruitment experience, preferably in technology; strong stakeholder communication and candidate sourcing skills.",
                            "6.00", "11.00", greener),
                    job("Customer Success Executive", "Pune, Maharashtra · Hybrid", "Full-time",
                            "Help business customers adopt a modern commerce platform. Guide onboarding, resolve questions, and share customer feedback with product teams.",
                            "1+ year in customer success or support; clear written and verbal communication; familiarity with CRM tools is helpful.",
                            "4.50", "8.00", greener),
                    internship("Marketing Research Intern", "Remote - India",
                            "Support research on customer needs and emerging consumer trends. Work with the marketing team on surveys, competitor reviews, and concise insight reports.",
                            "Currently pursuing a degree in business, communications, or a related field; curiosity, research skills, and comfort with spreadsheets.",
                            "12000", "20000", greener),
                    internship("Frontend Development Intern", "Hyderabad, Telangana · Hybrid",
                            "Contribute to responsive web features with guidance from experienced developers. Learn component design, accessibility, code review, and frontend testing.",
                            "Currently pursuing computer science or a related degree; familiarity with HTML, CSS, and JavaScript; React knowledge is a plus.",
                            "18000", "30000", northstar),
                    job("Cloud Platform Engineer", "Bengaluru, Karnataka · Hybrid", "Full-time",
                            "Build and operate cloud infrastructure for customer-facing services used across India. Improve deployment reliability, observability, and platform self-service for engineering teams.",
                            "3+ years with AWS or Azure, Linux, containers, and infrastructure as code; scripting and production incident experience preferred.",
                            "14.00", "24.00", northstar),
                    job("Business Analyst", "Noida, Uttar Pradesh · Hybrid", "Full-time",
                            "Work with business and technology teams to turn operational needs into clear requirements. Map workflows, analyze data, and support delivery from discovery through launch.",
                            "2+ years in business analysis; strong documentation and stakeholder skills; Excel and SQL experience are useful.",
                            "7.00", "12.00", greener),
                    job("QA Automation Engineer", "Chennai, Tamil Nadu · Hybrid", "Full-time",
                            "Help ship dependable digital products by building automated tests and improving release quality. Work closely with developers and product partners across the delivery lifecycle.",
                            "2+ years in software testing; experience with Selenium, Playwright, or Cypress; familiarity with API testing and CI pipelines.",
                            "8.00", "15.00", northstar),
                    job("Finance and Accounts Executive", "Ahmedabad, Gujarat", "Full-time",
                            "Support accurate day-to-day finance operations for a growing Indian business. Assist with reconciliations, invoices, expense tracking, and monthly reporting.",
                            "1+ year in accounting or finance operations; familiarity with GST documentation, spreadsheets, and accounting software.",
                            "3.60", "6.00", greener),
                    internship("Human Resources Intern", "Mumbai, Maharashtra · On-site",
                            "Support an inclusive candidate and employee experience. Assist with interview coordination, onboarding materials, and people-team projects with structured mentorship.",
                            "Currently pursuing a degree in human resources, business, or a related field; organized, discreet, and comfortable communicating with people.",
                            "10000", "18000", greener),
                    internship("Data Science Intern", "Remote - India",
                            "Explore real datasets with the analytics team and help prototype useful insights. Get mentorship in data preparation, evaluation, and communicating results.",
                            "Currently pursuing a degree in data science, statistics, computer science, or a related field; Python and basic statistics knowledge required.",
                            "20000", "35000", northstar)
            );
            for (JobPosting posting : postings) {
                JobPosting existing = jobRepository.findByTitleAndEmployer_Id(
                        posting.getTitle(), posting.getEmployer().getId()).orElse(null);
                if (existing == null) {
                    jobRepository.save(posting);
                } else if (existing.getSalaryMinLpa() == null && existing.getSalaryMaxLpa() == null
                        && existing.getStipendMinMonthly() == null && existing.getStipendMaxMonthly() == null
                        && isLegacySampleLocation(existing.getLocation())) {
                    existing.setLocation(posting.getLocation());
                    existing.setEmploymentType(posting.getEmploymentType());
                    existing.setDescription(posting.getDescription());
                    existing.setRequirements(posting.getRequirements());
                    existing.setSalaryMinLpa(posting.getSalaryMinLpa());
                    existing.setSalaryMaxLpa(posting.getSalaryMaxLpa());
                    existing.setStipendMinMonthly(posting.getStipendMinMonthly());
                    existing.setStipendMaxMonthly(posting.getStipendMaxMonthly());
                    jobRepository.save(existing);
                }
                if (existing != null && isDemoJob(existing.getTitle())) {
                    boolean changed = false;
                    if (existing.getWorkMode() == null) {
                        existing.setWorkMode(posting.getWorkMode());
                        changed = true;
                    }
                    if (existing.getMinExperienceYears() == null || existing.getMaxExperienceYears() == null) {
                        existing.setMinExperienceYears(posting.getMinExperienceYears());
                        existing.setMaxExperienceYears(posting.getMaxExperienceYears());
                        changed = true;
                    }
                    if (changed) jobRepository.save(existing);
                }
            }
        };
    }

    private void localizeLegacyPostings(
            JobRepository jobRepository, UserAccount northstar, UserAccount greener) {
        updateLegacyPosting(jobRepository, northstar, "Frontend Engineer", "New York, NY",
                "Bengaluru, Karnataka", "8.00", "16.00",
                "Build fast, accessible interfaces for customers across India using React. Work closely with product design and backend teams.");
        updateLegacyPosting(jobRepository, northstar, "Product Manager, Marketplace", "Hybrid · Austin, TX",
                "Hyderabad, Telangana · Hybrid", "16.00", "28.00",
                "Guide the roadmap for a growing Indian marketplace. Use customer research and product data to prioritize improvements and deliver measurable outcomes.");
        updateLegacyPosting(jobRepository, greener, "Sustainability Marketing Associate", "Remote",
                "Mumbai, Maharashtra · Hybrid", "5.00", "9.00",
                "Grow the reach of an Indian sustainable-goods brand. Plan digital campaigns, create clear content, and learn from customer insights.");
    }

    private void updateLegacyPosting(
            JobRepository jobRepository,
            UserAccount employer,
            String title,
            String previousLocation,
            String location,
            String salaryMinLpa,
            String salaryMaxLpa,
            String description) {
        jobRepository.findByTitleAndEmployer_Id(title, employer.getId())
                .filter(job -> previousLocation.equals(job.getLocation()))
                .ifPresent(job -> {
                    job.setLocation(location);
                    job.setDescription(description);
                    job.setSalaryMinLpa(new BigDecimal(salaryMinLpa));
                    job.setSalaryMaxLpa(new BigDecimal(salaryMaxLpa));
                    job.setWorkMode(inferWorkMode(location));
                    setLegacyExperience(job, title);
                    jobRepository.save(job);
                });
    }

    private void localizeLegacyInternship(JobRepository jobRepository, UserAccount employer) {
        jobRepository.findByTitleAndEmployer_Id("Software Engineering Intern", employer.getId())
                .filter(job -> job.getStipendMinMonthly() == null
                        && job.getStipendMaxMonthly() == null
                        && ("Hybrid · Boston, MA".equals(job.getLocation())
                        || "Chennai, Tamil Nadu".equals(job.getLocation())
                        || "Chennai, Tamil Nadu · On-site".equals(job.getLocation())))
                .ifPresent(job -> {
                    job.setLocation("Chennai, Tamil Nadu · On-site");
                    job.setDescription("Learn with an experienced engineering team and contribute to real product work. This structured internship includes mentorship, code reviews, and a scoped project.");
                    job.setRequirements("Currently pursuing a degree in computer science or a related field; programming fundamentals; curiosity and willingness to learn. Internship is intended for students and is not a permanent employee position.");
                    job.setSalaryMinLpa(null);
                    job.setSalaryMaxLpa(null);
                    job.setStipendMinMonthly(new BigDecimal("20000"));
                    job.setStipendMaxMonthly(new BigDecimal("35000"));
                    job.setWorkMode(WorkMode.ONSITE);
                    setExperience(job, 0, 1);
                    jobRepository.save(job);
                });
    }

    private boolean isLegacySampleLocation(String location) {
        return List.of("Remote", "New York, NY", "Hybrid · Austin, TX",
                "Chicago, IL", "Hybrid · Boston, MA").contains(location);
    }

    private boolean isDemoJob(String title) {
        return List.of("Senior Product Designer", "Java Spring Boot Developer",
                "Frontend React Developer", "Digital Marketing Associate",
                "Operations Coordinator", "Software Engineering Intern", "Data Analyst",
                "Product Manager", "Talent Acquisition Specialist", "Customer Success Executive",
                "Marketing Research Intern", "Frontend Development Intern", "Cloud Platform Engineer",
                "Business Analyst", "QA Automation Engineer", "Finance and Accounts Executive",
                "Human Resources Intern", "Data Science Intern").contains(title);
    }

    private UserAccount findOrCreateEmployer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String fullName,
            String companyName) {
        UserAccount existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.getRole() != Role.EMPLOYER) {
                throw new IllegalStateException("Cannot seed demo jobs: " + email + " is not an employer account");
            }
            if (("Northstar Studio".equals(existing.getCompanyName())
                    || "Greener Goods".equals(existing.getCompanyName()))
                    && !companyName.equals(existing.getCompanyName())) {
                existing.setCompanyName(companyName);
                return userRepository.save(existing);
            }
            return existing;
        }
        UserAccount employer = new UserAccount();
        employer.setEmail(email);
        employer.setFullName(fullName);
        employer.setCompanyName(companyName);
        employer.setRole(Role.EMPLOYER);
        employer.setPasswordHash(passwordEncoder.encode("DemoPass123!"));
        return userRepository.save(employer);
    }

    private JobPosting job(
            String title,
            String location,
            String employmentType,
            String description,
            String requirements,
            String salaryMinLpa,
            String salaryMaxLpa,
            UserAccount employer) {
        JobPosting posting = new JobPosting();
        posting.setTitle(title);
        posting.setLocation(location);
        posting.setEmploymentType(employmentType);
        posting.setWorkMode(inferWorkMode(location));
        switch (title) {
            case "Senior Product Designer" -> setExperience(posting, 5, 8);
            case "Java Spring Boot Developer" -> setExperience(posting, 3, 6);
            case "Frontend React Developer" -> setExperience(posting, 2, 5);
            case "Digital Marketing Associate" -> setExperience(posting, 2, 4);
            case "Operations Coordinator" -> setExperience(posting, 1, 3);
            case "Software Engineering Intern" -> setExperience(posting, 0, 1);
            case "Data Analyst" -> setExperience(posting, 2, 4);
            case "Product Manager" -> setExperience(posting, 4, 7);
            case "Talent Acquisition Specialist" -> setExperience(posting, 3, 5);
            case "Customer Success Executive" -> setExperience(posting, 1, 3);
            case "Marketing Research Intern", "Frontend Development Intern" -> setExperience(posting, 0, 1);
            case "Cloud Platform Engineer" -> setExperience(posting, 3, 6);
            case "Business Analyst" -> setExperience(posting, 2, 4);
            case "QA Automation Engineer" -> setExperience(posting, 2, 5);
            case "Finance and Accounts Executive" -> setExperience(posting, 1, 3);
            case "Human Resources Intern", "Data Science Intern" -> setExperience(posting, 0, 1);
            default -> { }
        }
        posting.setDescription(description);
        posting.setRequirements(requirements);
        posting.setSalaryMinLpa(salaryMinLpa == null ? null : new BigDecimal(salaryMinLpa));
        posting.setSalaryMaxLpa(salaryMaxLpa == null ? null : new BigDecimal(salaryMaxLpa));
        posting.setStatus(JobStatus.OPEN);
        posting.setEmployer(employer);
        return posting;
    }

    private WorkMode inferWorkMode(String location) {
        String normalized = location.toLowerCase(Locale.ROOT);
        if (normalized.contains("remote")) return WorkMode.REMOTE;
        if (normalized.contains("hybrid")) return WorkMode.HYBRID;
        return WorkMode.ONSITE;
    }

    private void setExperience(JobPosting posting, int minimum, int maximum) {
        posting.setMinExperienceYears(minimum);
        posting.setMaxExperienceYears(maximum);
    }

    private void setLegacyExperience(JobPosting posting, String title) {
        switch (title) {
            case "Frontend Engineer" -> setExperience(posting, 2, 5);
            case "Product Manager, Marketplace" -> setExperience(posting, 3, 6);
            case "Sustainability Marketing Associate" -> setExperience(posting, 2, 4);
            default -> { }
        }
    }

    private JobPosting internship(
            String title,
            String location,
            String description,
            String requirements,
            String stipendMinMonthly,
            String stipendMaxMonthly,
            UserAccount employer) {
        JobPosting posting = job(title, location, "Internship", description, requirements,
                null, null, employer);
        posting.setStipendMinMonthly(new BigDecimal(stipendMinMonthly));
        posting.setStipendMaxMonthly(new BigDecimal(stipendMaxMonthly));
        return posting;
    }
}
