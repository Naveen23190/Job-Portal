import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowRight, BriefcaseBusiness, Building2, Check, ChevronDown, ChevronLeft, ChevronRight, Heart, MapPin, Search, Sparkles, Users, X } from 'lucide-react'
import { API_URL, api } from './api'

const savedSession = JSON.parse(localStorage.getItem('pathway-session') || 'null')
const indiaDate = new Intl.DateTimeFormat('en-IN', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  timeZone: 'Asia/Kolkata',
})
const lpaNumber = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 })
const inrNumber = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
})
const interviewDate = new Intl.DateTimeFormat('en-IN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})
const companyLogos = {
  'northstar technologies': '/company-logos/northstar-technologies.svg',
  'greener goods india': '/company-logos/greener-goods-india.svg',
}

function readSavedJobs() {
  try {
    const saved = JSON.parse(localStorage.getItem('pathway-saved-jobs') || '[]')
    return Array.isArray(saved) ? saved.filter((id) => Number.isInteger(id)) : []
  } catch {
    return []
  }
}

function formatSalary(job) {
  if (job.employmentType?.toLowerCase() === 'internship') {
    if (job.stipendMinMonthly == null && job.stipendMaxMonthly == null) return null
    const minimum = job.stipendMinMonthly == null ? null : inrNumber.format(job.stipendMinMonthly)
    const maximum = job.stipendMaxMonthly == null ? null : inrNumber.format(job.stipendMaxMonthly)
    return `${minimum && maximum ? `${minimum}–${maximum}` : minimum || maximum} / month`
  }
  if (job.salaryMinLpa == null && job.salaryMaxLpa == null) return null
  const minimum = job.salaryMinLpa == null ? null : lpaNumber.format(job.salaryMinLpa)
  const maximum = job.salaryMaxLpa == null ? null : lpaNumber.format(job.salaryMaxLpa)
  if (minimum && maximum) return `₹${minimum}–${maximum} LPA CTC`
  return `₹${minimum || maximum} LPA CTC`
}

function profileCompletion(profile) {
  if (!profile) return 0
  const completedSections = [
    Boolean(profile.fullName?.trim()),
    Boolean(profile.bio?.trim()),
    Boolean(profile.resumeUrl?.trim()),
    Boolean(profile.skills?.length),
    Boolean(profile.education?.length),
  ].filter(Boolean).length
  return Math.round((completedSections / 5) * 100)
}

function CompanyLogo({ companyName = 'Company' }) {
  const logoPath = companyLogos[companyName.trim().toLocaleLowerCase()]
  if (logoPath) {
    return <img className="company-logo" src={logoPath} alt={`${companyName} logo`} />
  }

  const palettes = [
    ['#225c79', '#42a6a0'],
    ['#574888', '#9a74c1'],
    ['#8a5732', '#d49a57'],
    ['#2f6b51', '#77aa73'],
    ['#81475d', '#cc7b79'],
    ['#375787', '#6e9ac4'],
  ]
  const paletteIndex = Array.from(companyName).reduce((total, character) => total + character.charCodeAt(0), 0) % palettes.length
  const initials = companyName.split(/\s+/).filter(Boolean).slice(0, 2).map((word) => word[0].toUpperCase()).join('')
  const [startColor, endColor] = palettes[paletteIndex]

  return (
    <svg className="company-logo" viewBox="0 0 48 48" role="img" aria-label={`${companyName} company mark`}>
      <rect width="48" height="48" rx="13" fill={startColor} />
      <path d="M0 34 34 0h14v14L14 48H0z" fill={endColor} fillOpacity=".55" />
      <path d="M11 29 23.8 9 37 29h-7l-6.2-9.4L17.7 29z" fill="#fff" fillOpacity=".9" />
      <circle cx="35.5" cy="12.5" r="3" fill="#f4d17b" />
      <text x="24" y="40" textAnchor="middle" fill="#fff" fontSize="7" fontWeight="700" letterSpacing=".8">{initials}</text>
    </svg>
  )
}

function App() {
  const jobsPerPage = 9
  const [session, setSession] = useState(savedSession)
  const [jobs, setJobs] = useState([])
  const [jobPage, setJobPage] = useState(0)
  const [jobPageInfo, setJobPageInfo] = useState({ totalPages: 0, totalElements: 0, size: jobsPerPage })
  const [isJobsLoading, setIsJobsLoading] = useState(true)
  const [savedJobs, setSavedJobs] = useState(readSavedJobs)
  const [showSavedJobs, setShowSavedJobs] = useState(false)
  const [employerJobs, setEmployerJobs] = useState([])
  const [applications, setApplications] = useState([])
  const [interviews, setInterviews] = useState([])
  const [liveStatus, setLiveStatus] = useState('offline')
  const [applicantSearch, setApplicantSearch] = useState('')
  const [applicationStatusFilter, setApplicationStatusFilter] = useState('')
  const [search, setSearch] = useState('')
  const [location, setLocation] = useState('')
  const [filters, setFilters] = useState({
    workMode: '',
    employmentType: '',
    experienceYears: '',
    minSalaryLpa: '',
    maxSalaryLpa: '',
  })
  const [appliedFilters, setAppliedFilters] = useState({})
  const [activeTab, setActiveTab] = useState('Find jobs')
  const [modal, setModal] = useState('')
  const [selectedJob, setSelectedJob] = useState(null)
  const [selectedApplication, setSelectedApplication] = useState(null)
  const [editingJob, setEditingJob] = useState(null)
  const [compensationType, setCompensationType] = useState('salary')
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [profile, setProfile] = useState(null)
  const jobsRequestId = useRef(0)

  const refreshJobs = useCallback(async (jobFilters = {}, page = 0, savedJobIds = null) => {
    const requestId = ++jobsRequestId.current
    setIsJobsLoading(true)
    try {
      const params = new URLSearchParams()
      params.set('page', page)
      params.set('size', jobsPerPage)
      Object.entries(jobFilters).forEach(([key, value]) => {
        if (value !== '' && value != null) params.set(key, value)
      })
      const endpoint = savedJobIds === null ? '/jobs' : '/jobs/saved'
      savedJobIds?.forEach((id) => params.append('ids', id))
      const result = await api(`${endpoint}?${params}`)
      if (requestId !== jobsRequestId.current) return
      setJobs(result.content)
      setJobPageInfo({
        totalPages: result.totalPages,
        totalElements: result.totalElements,
        size: result.size,
      })
      if (page > 0 && page >= result.totalPages) {
        setJobPage(Math.max(0, result.totalPages - 1))
      }
    } catch (exception) {
      if (requestId !== jobsRequestId.current) return
      setError(exception.message)
    } finally {
      if (requestId === jobsRequestId.current) setIsJobsLoading(false)
    }
  }, [])

  const refreshApplications = useCallback(async () => {
    if (!session || session.role !== 'JOB_SEEKER') return
    try {
      const [applicationRows, interviewRows] = await Promise.all([
        api('/applications/me', { token: session.token }),
        api('/interviews/me', { token: session.token }),
      ])
      setApplications(applicationRows)
      setInterviews(interviewRows)
    } catch (exception) {
      setError(exception.message)
    }
  }, [session])

  useEffect(() => {
    if (showSavedJobs) refreshJobs({}, jobPage, savedJobs)
    else refreshJobs(appliedFilters, jobPage)
  }, [refreshJobs, appliedFilters, showSavedJobs, jobPage, savedJobs])
  useEffect(() => { refreshApplications() }, [refreshApplications])

  function applyJobFilters(event) {
    event.preventDefault()
    const compensationFilters = filters.employmentType === 'Internship'
      ? { minStipendMonthly: filters.minSalaryLpa, maxStipendMonthly: filters.maxSalaryLpa }
      : { minSalaryLpa: filters.minSalaryLpa, maxSalaryLpa: filters.maxSalaryLpa }
    const { minSalaryLpa, maxSalaryLpa, ...otherFilters } = filters
    setAppliedFilters({
      keyword: search.trim(),
      location: location.trim(),
      ...otherFilters,
      ...compensationFilters,
    })
    setJobPage(0)
  }

  function clearJobFilters() {
    setSearch('')
    setLocation('')
    setFilters({
      workMode: '',
      employmentType: '',
      experienceYears: '',
      minSalaryLpa: '',
      maxSalaryLpa: '',
    })
    setAppliedFilters({})
    setJobPage(0)
    setShowSavedJobs(false)
  }

  function toggleSavedJob(jobId) {
    if (!savedJobs.includes(jobId) && savedJobs.length >= 100) {
      setError('You can save up to 100 roles.')
      return
    }
    const nextSavedJobs = savedJobs.includes(jobId)
      ? savedJobs.filter((id) => id !== jobId)
      : [...savedJobs, jobId]
    try {
      localStorage.setItem('pathway-saved-jobs', JSON.stringify(nextSavedJobs))
      setSavedJobs(nextSavedJobs)
      setNotice(nextSavedJobs.includes(jobId) ? 'Role saved to your list.' : 'Role removed from your saved list.')
    } catch {
      setError('Could not save this role in your browser. Check your browser storage settings.')
    }
  }

  function signOut() {
    localStorage.removeItem('pathway-session')
    setSession(null)
    setApplications([])
    setInterviews([])
    setActiveTab('Find jobs')
    setNotice('You have signed out.')
  }

  async function submitAuth(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    const data = new FormData(event.currentTarget)
    const isRegister = modal === 'register'
    const payload = {
      email: data.get('email'),
      password: data.get('password'),
      ...(isRegister ? {
        fullName: data.get('fullName'),
        role: data.get('role'),
        companyName: data.get('companyName'),
      } : {}),
    }
    try {
      const result = await api(`/auth/${isRegister ? 'register' : 'login'}`, {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      localStorage.setItem('pathway-session', JSON.stringify(result))
      setSession(result)
      setModal('')
      setNotice(`Welcome${result.fullName ? `, ${result.fullName.split(' ')[0]}` : ''}!`)
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  async function submitApplication(event) {
    event.preventDefault()
    if (!session) {
      setModal('login')
      return
    }
    setBusy(true)
    setError('')
    const coverLetter = new FormData(event.currentTarget).get('coverLetter')
    try {
      await api(`/jobs/${selectedJob.id}/applications`, {
        method: 'POST',
        token: session.token,
        body: JSON.stringify({ coverLetter }),
      })
      setNotice('Application submitted successfully.')
      setModal('')
      setSelectedJob(null)
      refreshApplications()
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  async function submitJob(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    const data = new FormData(event.currentTarget)
    const job = Object.fromEntries(data.entries())
    const isInternship = job.employmentType.toLowerCase() === 'internship'
    job.workMode = job.workMode || 'ONSITE'
    job.minExperienceYears = job.minExperienceYears ? Number(job.minExperienceYears) : null
    job.maxExperienceYears = job.maxExperienceYears ? Number(job.maxExperienceYears) : null
    job.salaryMinLpa = !isInternship && job.salaryMinLpa ? Number(job.salaryMinLpa) : null
    job.salaryMaxLpa = !isInternship && job.salaryMaxLpa ? Number(job.salaryMaxLpa) : null
    job.stipendMinMonthly = isInternship && job.stipendMinMonthly ? Number(job.stipendMinMonthly) : null
    job.stipendMaxMonthly = isInternship && job.stipendMaxMonthly ? Number(job.stipendMaxMonthly) : null
    try {
      await api(editingJob ? `/jobs/${editingJob.id}` : '/jobs', {
        token: session.token,
        method: editingJob ? 'PUT' : 'POST',
        body: JSON.stringify(job),
      })
      setNotice(editingJob ? 'Your job post has been updated.' : 'Your role is live. Candidates can now apply.')
      setModal('')
      setEditingJob(null)
      refreshJobs(appliedFilters, jobPage)
      loadEmployerJobs()
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  async function updateApplicationStatus(applicationId, status) {
    setBusy(true)
    setError('')
    try {
      await api(`/applications/${applicationId}/status`, {
        token: session.token,
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })
      setNotice('Candidate status updated.')
      loadEmployerApplications()
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  async function withdrawApplication(application) {
    if (!window.confirm(`Withdraw your application for ${application.jobTitle}?`)) return
    setBusy(true)
    setError('')
    try {
      const updated = await api(`/applications/${application.id}/withdraw`, {
        token: session.token,
        method: 'PATCH',
      })
      setApplications((current) => current.map((item) => item.id === updated.id ? updated : item))
      setNotice('Your application has been withdrawn.')
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  const loadEmployerApplications = useCallback(async () => {
    try {
      const [employerJobs, interviewRows] = await Promise.all([
        api('/jobs/mine', { token: session.token }),
        api('/interviews/me', { token: session.token }),
      ])
      setEmployerJobs(employerJobs)
      const results = await Promise.all(employerJobs.map(async (job) => ({
        job,
        applications: await api(`/jobs/${job.id}/applications`, { token: session.token }),
      })))
      setApplications(results.flatMap(({ job, applications: rows }) =>
        rows.map((application) => ({ ...application, jobTitle: job.title }))))
      setInterviews(interviewRows)
    } catch (exception) {
      setError(exception.message)
    }
  }, [session])

  async function submitInterview(event) {
    event.preventDefault()
    if (!selectedApplication) return
    setBusy(true)
    setError('')
    const values = Object.fromEntries(new FormData(event.currentTarget).entries())
    try {
      const interview = await api(`/applications/${selectedApplication.id}/interviews`, {
        token: session.token,
        method: 'POST',
        body: JSON.stringify({
          proposedAt: new Date(values.proposedAt).toISOString(),
          durationMinutes: Number(values.durationMinutes),
          meetingUrl: values.meetingUrl,
          notes: values.notes,
        }),
      })
      setInterviews((current) => [
        interview,
        ...current.filter((existing) => existing.id !== interview.id),
      ])
      setApplications((current) => current.map((application) =>
        application.id === selectedApplication.id ? { ...application, status: 'INTERVIEW' } : application))
      setNotice('Interview proposal sent to the candidate.')
      setModal('')
      setSelectedApplication(null)
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  async function respondToInterview(interviewId, status) {
    setBusy(true)
    setError('')
    try {
      const updated = await api(`/interviews/${interviewId}/response`, {
        token: session.token,
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })
      setInterviews((current) => current.map((interview) =>
        interview.id === updated.id ? updated : interview))
      setNotice(`Interview ${status.toLowerCase()}.`)
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  async function loadEmployerJobs() {
    try {
      setEmployerJobs(await api('/jobs/mine', { token: session.token }))
    } catch (exception) {
      setError(exception.message)
    }
  }

  async function changeJobStatus(job) {
    const status = job.status === 'OPEN' ? 'CLOSED' : 'OPEN'
    setBusy(true)
    setError('')
    try {
      await api(`/jobs/${job.id}/status?status=${status}`, {
        token: session.token,
        method: 'PATCH',
      })
      setNotice(`Job ${status === 'OPEN' ? 'reopened' : 'closed'}.`)
      loadEmployerJobs()
      refreshJobs(appliedFilters, jobPage)
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  async function openProfile() {
    setError('')
    try {
      setProfile(await api('/profile/me', { token: session.token }))
      setModal('profile')
    } catch (exception) {
      setError(exception.message)
    }
  }

  async function submitProfile(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    const data = new FormData(event.currentTarget)
    const values = Object.fromEntries(data.entries())
    const splitEntries = (value) => value.split(/[\n,]/).map((entry) => entry.trim()).filter(Boolean)
    try {
      const updated = await api('/profile/me', {
        token: session.token,
        method: 'PUT',
        body: JSON.stringify({
          ...values,
          skills: splitEntries(values.skills || ''),
          education: splitEntries(values.education || ''),
        }),
      })
      const updatedSession = { ...session, fullName: updated.fullName, companyName: updated.companyName }
      setProfile(updated)
      setSession(updatedSession)
      localStorage.setItem('pathway-session', JSON.stringify(updatedSession))
      setNotice('Your profile has been saved.')
      setModal('')
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    if (activeTab === 'Applications' && session?.role === 'EMPLOYER') loadEmployerApplications()
  }, [activeTab, session?.role, loadEmployerApplications])
  useEffect(() => {
    if (activeTab === 'For employers' && session?.role === 'EMPLOYER') loadEmployerJobs()
  }, [activeTab, session])

  useEffect(() => {
    if (!session?.token) {
      setLiveStatus('offline')
      return undefined
    }
    const controller = new AbortController()
    const { signal } = controller
    let retryTimer

    async function reloadLiveData() {
      if (session.role === 'JOB_SEEKER') await refreshApplications()
      if (session.role === 'EMPLOYER') await loadEmployerApplications()
    }

    async function connect() {
      while (!signal.aborted) {
        setLiveStatus('connecting')
        try {
          const response = await fetch(`${API_URL}/events/stream`, {
            headers: {
              Accept: 'text/event-stream',
              Authorization: `Bearer ${session.token}`,
            },
            signal,
          })
          if (response.status === 401 || response.status === 403) {
            setLiveStatus('offline')
            return
          }
          if (!response.ok || !response.body) {
            throw new Error(`Live updates unavailable (${response.status})`)
          }

          const reader = response.body.getReader()
          const decoder = new TextDecoder()
          let buffer = ''
          while (!signal.aborted) {
            const { value, done } = await reader.read()
            if (done) throw new Error('Live update stream closed')
            buffer += decoder.decode(value, { stream: true })
            const blocks = buffer.split(/\r?\n\r?\n/)
            buffer = blocks.pop() || ''
            for (const block of blocks) {
              const eventName = block.split(/\r?\n/)
                .find((line) => line.startsWith('event:'))
                ?.slice(6).trim()
              if (eventName === 'connected') {
                setLiveStatus('live')
                await reloadLiveData()
              } else if (eventName === 'application-updated' || eventName === 'interview-updated') {
                await reloadLiveData()
              }
            }
          }
        } catch {
          if (signal.aborted) return
          setLiveStatus('reconnecting')
        }

        await new Promise((resolve) => {
          const finish = () => {
            signal.removeEventListener('abort', abortRetry)
            resolve()
          }
          const abortRetry = () => {
            window.clearTimeout(retryTimer)
            finish()
          }
          retryTimer = window.setTimeout(finish, 3000)
          signal.addEventListener('abort', abortRetry, { once: true })
        })
      }
    }

    connect()
    return () => {
      controller.abort()
      window.clearTimeout(retryTimer)
    }
  }, [session, refreshApplications, loadEmployerApplications])

  const isEmployer = session?.role === 'EMPLOYER'
  const showApplications = activeTab === 'Applications' && (session?.role === 'JOB_SEEKER' || isEmployer)
  const visibleApplications = isEmployer
    ? applications.filter((application) => {
      const searchTerm = applicantSearch.trim().toLocaleLowerCase()
      const matchesSearch = !searchTerm
        || [application.applicantName, application.applicantEmail, application.jobTitle]
          .some((value) => value?.toLocaleLowerCase().includes(searchTerm))
      return matchesSearch
        && (!applicationStatusFilter || application.status === applicationStatusFilter)
    })
    : applications
  const displayedJobs = showSavedJobs ? jobs.filter((job) => savedJobs.includes(job.id)) : jobs
  const title = activeTab === 'Applications' ? (isEmployer ? 'Review candidates' : 'Your applications') : activeTab === 'For employers' ? 'Build your next team' : showSavedJobs ? 'Your saved roles' : 'Find your next opportunity'
  const liveStatusLabel = liveStatus === 'live' ? 'Live updates' : liveStatus === 'reconnecting' ? 'Reconnecting' : liveStatus === 'connecting' ? 'Connecting' : 'Offline'

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#" onClick={() => setActiveTab('Find jobs')} aria-label="JobHire home"><span className="brand-mark"><BriefcaseBusiness size={19} /></span> JobHire<span className="brand-dot">.</span></a>
        <nav className="main-nav" aria-label="Main navigation">
          <button className={activeTab === 'Find jobs' ? 'nav-link active' : 'nav-link'} onClick={() => setActiveTab('Find jobs')}>Find jobs</button>
          {(session?.role === 'JOB_SEEKER' || isEmployer) && <button className={activeTab === 'Applications' ? 'nav-link active' : 'nav-link'} onClick={() => setActiveTab('Applications')}>Applications</button>}
          {isEmployer && <button className="nav-link" onClick={() => setActiveTab('For employers')}>For employers</button>}
          {session && <button className="nav-link profile-nav" onClick={openProfile}>Profile</button>}
        </nav>
        <div className="account-actions">
          {session ? <>
            <span className="user-greeting">Hi, {session.fullName.split(' ')[0]}</span>
            <button className="avatar" title={`${session.fullName} (${session.role})`}>{session.fullName.charAt(0).toUpperCase()}</button>
            <button className="button button-outline button-small" onClick={signOut}>Sign out</button>
          </> : <>
            <button className="button button-quiet" onClick={() => { setError(''); setModal('login') }}>Log in</button>
            <button className="button button-dark button-small" onClick={() => { setError(''); setModal('register') }}>Get started <ArrowRight size={15} /></button>
          </>}
        </div>
      </header>

      <main>
        <section className="hero">
          <div className="hero-copy">
            <div className="eyebrow"><span className="eyebrow-line" /> OPPORTUNITIES ACROSS INDIA</div>
            <h1>{title}<span className="title-period">.</span></h1>
            <p className="hero-subtitle">{activeTab === 'Applications' ? 'Keep every opportunity in sight and follow your progress.' : activeTab === 'For employers' ? 'Meet talented people ready to make a difference across India.' : showSavedJobs ? 'Keep your shortlisted opportunities close and come back when you are ready.' : 'Find meaningful work in India’s thriving cities, or discover a remote role.'}</p>
            {activeTab === 'For employers' ? (
              <button className="button button-dark hero-cta" onClick={() => { setEditingJob(null); setCompensationType('salary'); setModal('job') }}>Post a job <ArrowRight size={16} /></button>
            ) : !showApplications && !showSavedJobs ? (
              <form className="job-search-form" onSubmit={applyJobFilters}>
                <div className="search-panel">
                  <label className="search-field"><Search size={18} /><input placeholder="Role, skill or company" value={search} onChange={(event) => setSearch(event.target.value)} /></label>
                  <span className="search-divider" />
                  <label className="search-field location-field"><MapPin size={18} /><input placeholder="Indian city or remote" value={location} onChange={(event) => setLocation(event.target.value)} /></label>
                  <button className="button button-accent search-button" type="submit">Search jobs</button>
                </div>
                <div className="advanced-filters">
                  <label>Work mode<select value={filters.workMode} onChange={(event) => setFilters({ ...filters, workMode: event.target.value })}><option value="">Any work mode</option><option value="ONSITE">On-site</option><option value="HYBRID">Hybrid</option><option value="REMOTE">Remote</option></select></label>
                  <label>Job type<select value={filters.employmentType} onChange={(event) => setFilters({ ...filters, employmentType: event.target.value })}><option value="">Any job type</option><option>Full-time</option><option>Part-time</option><option>Contract</option><option>Internship</option></select></label>
                  <label>Experience<select value={filters.experienceYears} onChange={(event) => setFilters({ ...filters, experienceYears: event.target.value })}><option value="">Any experience</option><option value="0">Fresher / entry level</option><option value="1">1+ years</option><option value="2">2+ years</option><option value="3">3+ years</option><option value="5">5+ years</option><option value="8">8+ years</option></select></label>
                  <label>{filters.employmentType === 'Internship' ? 'Min stipend (₹ / month)' : 'Min CTC (₹ LPA)'}<input type="number" min="0" max={filters.employmentType === 'Internship' ? '1000000' : '1000'} step={filters.employmentType === 'Internship' ? '500' : '0.5'} placeholder="Any" value={filters.minSalaryLpa} onChange={(event) => setFilters({ ...filters, minSalaryLpa: event.target.value })} /></label>
                  <label>{filters.employmentType === 'Internship' ? 'Max stipend (₹ / month)' : 'Max CTC (₹ LPA)'}<input type="number" min="0" max={filters.employmentType === 'Internship' ? '1000000' : '1000'} step={filters.employmentType === 'Internship' ? '500' : '0.5'} placeholder="Any" value={filters.maxSalaryLpa} onChange={(event) => setFilters({ ...filters, maxSalaryLpa: event.target.value })} /></label>
                </div>
              </form>
            ) : null}
            <div className="hero-proof"><span><Users size={15} /> Opportunities across India</span><span className="proof-divider" /><span><Sparkles size={15} /> CTC in ₹ LPA · Intern stipends monthly</span></div>
          </div>
          <div className="hero-art" aria-hidden="true">
            <div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" />
            <div className="art-card art-card-back"><span className="art-mini-dot" /><span className="art-line long" /><span className="art-line" /></div>
            <div className="art-card art-card-front"><div className="art-card-top"><span className="art-icon"><Building2 size={18} /></span><span className="art-pill">HIRING NOW</span></div><div className="art-line long" /><div className="art-line medium" /><div className="art-bottom"><span className="art-avatar-stack"><i /><i /><i /></span><span>Good people, good work.</span></div></div>
            <div className="art-sticker"><span>✳</span><small>YOUR NEXT<br />CHAPTER</small></div>
            <div className="art-sparkle sparkle-a">✦</div><div className="art-sparkle sparkle-b">✳</div>
          </div>
        </section>

        <section className="content-section" id="open-roles">
          <div className="section-heading">
            <div><div className="section-kicker">{showApplications ? (isEmployer ? 'YOUR ROLES' : 'YOUR JOURNEY') : 'HANDPICKED FOR YOU'}</div><h2>{showApplications ? (isEmployer ? 'Candidate applications' : 'Application tracker') : activeTab === 'For employers' ? 'Manage your roles' : showSavedJobs ? 'Saved roles' : 'Open roles'} <span>{showApplications ? applications.length : activeTab === 'For employers' ? employerJobs.length : jobPageInfo.totalElements}</span></h2></div>
            {showApplications && <span className={`live-indicator live-${liveStatus}`} role="status" aria-live="polite"><span />{liveStatusLabel}</span>}
            {!showApplications && activeTab !== 'For employers' && <div className="job-list-actions"><button className={showSavedJobs ? 'saved-filter active' : 'saved-filter'} onClick={() => { setJobPage(0); setShowSavedJobs(!showSavedJobs) }}><Heart size={15} fill={showSavedJobs ? 'currentColor' : 'none'} /> Saved <span>{savedJobs.length}</span></button><button className="filter-button" onClick={clearJobFilters}>Clear filters <X size={14} /></button></div>}
          </div>

          {error && <div className="alert alert-error" role="alert">{error}<button onClick={() => setError('')} aria-label="Dismiss error"><X size={16} /></button></div>}
          {notice && <div className="alert alert-success" role="status"><Check size={16} />{notice}<button onClick={() => setNotice('')} aria-label="Dismiss message"><X size={16} /></button></div>}

          {showApplications ? (
            applications.length ? <>
              {isEmployer && <div className="applicant-filters" role="search" aria-label="Filter candidate applications">
                <label className="applicant-search"><Search size={16} /><input type="search" placeholder="Search candidate, email, or role" value={applicantSearch} onChange={(event) => setApplicantSearch(event.target.value)} /></label>
                <label className="applicant-status-filter"><span>Status</span><select value={applicationStatusFilter} onChange={(event) => setApplicationStatusFilter(event.target.value)}><option value="">All statuses</option>{['SUBMITTED', 'REVIEWING', 'INTERVIEW', 'REJECTED', 'HIRED', 'WITHDRAWN'].map((status) => <option key={status} value={status}>{status.replace('_', ' ')}</option>)}</select><ChevronDown size={13} /></label>
                {(applicantSearch || applicationStatusFilter) && <button className="clear-applicant-filters" onClick={() => { setApplicantSearch(''); setApplicationStatusFilter('') }}>Clear</button>}
                <span className="applicant-result-count" aria-live="polite">Showing {visibleApplications.length} of {applications.length}</span>
              </div>}
              {visibleApplications.length ? <div className="application-list">{visibleApplications.map((application) => (
              <article className="application-card" key={application.id}>
                <div className="company-icon"><BriefcaseBusiness size={19} /></div>
                <div className="application-info"><h3>{application.jobTitle}</h3><p>{isEmployer ? <>{application.applicantName || 'Applicant'}{application.applicantEmail && ` · ${application.applicantEmail}`}</> : 'Your application'} · Applied {indiaDate.format(new Date(application.appliedAt))} {isEmployer && application.resumeUrl?.match(/^https?:\/\//i) && <>· <a className="resume-link" href={application.resumeUrl} target="_blank" rel="noreferrer">Resume</a></>}</p>{isEmployer && application.coverLetter && <p className="application-cover-letter">{application.coverLetter}</p>}
                  {interviews.filter((interview) => interview.applicationId === application.id).map((interview) => (
                    <div className="interview-summary" key={interview.id}>
                      <p><strong>Interview {interview.status.toLowerCase()}</strong> · {interviewDate.format(new Date(interview.proposedAt))} · {interview.durationMinutes} min</p>
                      {interview.notes && <p>{interview.notes}</p>}
                      {interview.meetingUrl && <a href={interview.meetingUrl} target="_blank" rel="noreferrer">Join meeting <ArrowRight size={13} /></a>}
                      {!isEmployer && interview.status === 'PROPOSED' && <div className="interview-actions">
                        <button className="button button-accent button-small" disabled={busy} onClick={() => respondToInterview(interview.id, 'ACCEPTED')}>Accept</button>
                        <button className="button button-outline button-small" disabled={busy} onClick={() => respondToInterview(interview.id, 'DECLINED')}>Decline</button>
                      </div>}
                    </div>
                  ))}
                </div>
                <span className={`status-badge status-${application.status.toLowerCase()}`}>{application.status.replace('_', ' ')}</span>
                {!isEmployer && ['SUBMITTED', 'REVIEWING'].includes(application.status) && <button className="withdraw-action" disabled={busy} onClick={() => withdrawApplication(application)}>Withdraw</button>}
                {isEmployer && application.status !== 'WITHDRAWN' && <label className="status-select"><span>Update</span><select value={application.status} disabled={busy} onChange={(event) => updateApplicationStatus(application.id, event.target.value)}>{['SUBMITTED', 'REVIEWING', 'INTERVIEW', 'REJECTED', 'HIRED'].map((status) => <option key={status} value={status}>{status}</option>)}</select><ChevronDown size={13} /></label>}
                {isEmployer && ['SUBMITTED', 'REVIEWING', 'INTERVIEW'].includes(application.status)
                  && !interviews.some((interview) => interview.applicationId === application.id && interview.status === 'PROPOSED')
                  && <button className="schedule-interview-action" disabled={busy} onClick={() => { setSelectedApplication(application); setModal('interview'); setError('') }}>Schedule interview</button>}
              </article>
              ))}</div> : <div className="empty-state applicant-empty"><Search size={25} /><h3>No matching candidates</h3><p>Try another name, email, job title, or status filter.</p><button className="button button-outline" onClick={() => { setApplicantSearch(''); setApplicationStatusFilter('') }}>Clear candidate filters</button></div>}
            </> : <div className="empty-state"><BriefcaseBusiness size={26} /><h3>{isEmployer ? 'No candidates yet' : 'No applications yet'}</h3><p>{isEmployer ? 'Applications to your open roles will show here.' : 'When you apply for a role, you can follow its progress here.'}</p><button className="button button-dark" onClick={() => setActiveTab(isEmployer ? 'For employers' : 'Find jobs')}>{isEmployer ? 'Manage job posts' : 'Explore open roles'}</button></div>
          ) : activeTab === 'For employers' ? (
            <div className="employer-dashboard">
              <div className="employer-banner"><div className="employer-icon"><Building2 size={25} /></div><div><h3>Bring your next great teammate on board.</h3><p>Write a clear role description and start meeting candidates.</p></div><button className="button button-accent" onClick={() => { setEditingJob(null); setCompensationType('salary'); setModal('job') }}>Post a job <ArrowRight size={15} /></button></div>
              {employerJobs.map((job) => <article className="employer-job-row" key={job.id}>
                <CompanyLogo companyName={job.companyName || 'Company'} />
                <div className="employer-job-info"><h3>{job.title}</h3><p>{job.location} · {job.employmentType} · {job.companyName}{formatSalary(job) && ` · ${formatSalary(job)}`}</p></div>
                <span className={`status-badge status-${job.status.toLowerCase()}`}>{job.status}</span>
                <div className="employer-job-actions">
                  <button className="text-action" onClick={() => setActiveTab('Applications')}>Applicants</button>
                  <button className="text-action" onClick={() => { setEditingJob(job); setCompensationType(job.employmentType.toLowerCase() === 'internship' ? 'stipend' : 'salary'); setModal('job') }}>Edit</button>
                  <button className="text-action" disabled={busy} onClick={() => changeJobStatus(job)}>{job.status === 'OPEN' ? 'Close' : 'Reopen'}</button>
                </div>
              </article>)}
              {!employerJobs.length && <div className="empty-state"><BriefcaseBusiness size={25} /><h3>No job posts yet</h3><p>Create your first post to start meeting candidates.</p></div>}
            </div>
          ) : (
            <div className="job-grid" aria-busy={isJobsLoading}>
              {isJobsLoading && !jobs.length ? Array.from({ length: jobsPerPage }, (_, index) => <article className="job-card job-skeleton" key={`skeleton-${index}`} aria-hidden="true"><span /><span /><span /><span /><span /></article>) : displayedJobs.map((job) => <article className="job-card" key={job.id}>
                <div className="job-card-top"><CompanyLogo companyName={job.companyName || 'Company'} /><button className={savedJobs.includes(job.id) ? 'save-button is-saved' : 'save-button'} aria-label={savedJobs.includes(job.id) ? `Remove ${job.title} from saved roles` : `Save ${job.title}`} aria-pressed={savedJobs.includes(job.id)} onClick={() => toggleSavedJob(job.id)}><Heart size={18} fill={savedJobs.includes(job.id) ? 'currentColor' : 'none'} /></button></div>
                <div className="job-company">{job.companyName || 'Growing company'}</div>
                <h3>{job.title}</h3>
                <div className="job-meta"><span><MapPin size={14} />{job.location}</span><span><BriefcaseBusiness size={14} />{job.employmentType}</span></div>
                <div className="job-facts"><span>{job.workMode === 'REMOTE' ? 'Remote' : job.workMode === 'HYBRID' ? 'Hybrid' : job.workMode === 'ONSITE' ? 'On-site' : job.location}</span>{job.minExperienceYears != null && <span>{job.minExperienceYears}{job.maxExperienceYears != null ? `–${job.maxExperienceYears}` : '+'} yrs experience</span>}</div>
                {formatSalary(job) && <div className="salary-range">{formatSalary(job)} <span>{job.employmentType?.toLowerCase() === 'internship' ? 'stipend' : 'annual CTC'}</span></div>}
                <p className="job-description">{job.description}</p>
                <div className="job-card-footer"><span className="posted-date">{indiaDate.format(new Date(job.createdAt))}</span><button className="apply-link" onClick={() => { setSelectedJob(job); setError(''); setModal(session ? 'apply' : 'login') }}>View & apply <ArrowRight size={15} /></button></div>
              </article>)}
              {!isJobsLoading && !displayedJobs.length && <div className="empty-state empty-jobs"><Search size={25} /><h3>{showSavedJobs ? 'No saved roles yet' : 'No matching roles'}</h3><p>{showSavedJobs ? 'Tap the heart on any job card to save a role for later.' : 'Try adjusting your search, or check back soon for new openings.'}</p><button className="button button-outline" onClick={showSavedJobs ? () => { setJobPage(0); setShowSavedJobs(false) } : clearJobFilters}>{showSavedJobs ? 'Browse open roles' : 'Reset search'}</button></div>}
            </div>
          )}
          {!showApplications && activeTab !== 'For employers' && !isJobsLoading && jobPageInfo.totalPages > 1 && <nav className="pagination" aria-label="Job results pages">
            <span>Showing {jobPage * jobPageInfo.size + 1}–{Math.min((jobPage + 1) * jobPageInfo.size, jobPageInfo.totalElements)} of {jobPageInfo.totalElements} roles</span>
            <div className="pagination-controls">
              <button className="pagination-button" onClick={() => setJobPage(Math.max(0, jobPage - 1))} disabled={jobPage === 0} aria-label="Previous page"><ChevronLeft size={16} /> Previous</button>
              <span className="pagination-current">Page {jobPage + 1} of {jobPageInfo.totalPages}</span>
              <button className="pagination-button" onClick={() => setJobPage(Math.min(jobPageInfo.totalPages - 1, jobPage + 1))} disabled={jobPage >= jobPageInfo.totalPages - 1} aria-label="Next page">Next <ChevronRight size={16} /></button>
            </div>
          </nav>}
        </section>
      </main>
      <footer><a className="brand footer-brand" href="#" onClick={() => setActiveTab('Find jobs')} aria-label="JobHire home"><span className="brand-mark"><BriefcaseBusiness size={16} /></span> JobHire<span className="brand-dot">.</span></a><span>Find your next opportunity across India.</span><span>© {new Date().getFullYear()} JobHire</span><span className="developer-credit">Developed by <strong>Naveen Kumar</strong></span></footer>

      {modal && <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) { setModal(''); setError('') } }}>
        <section className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          <button className="modal-close" onClick={() => { setModal(''); setError('') }} aria-label="Close dialog"><X size={19} /></button>
          <div className="modal-mark"><BriefcaseBusiness size={19} /></div>
          {modal === 'login' || modal === 'register' ? <>
            <div className="section-kicker">{modal === 'login' ? 'WELCOME BACK' : 'YOUR NEXT CHAPTER'}</div>
            <h2 id="modal-title">{modal === 'login' ? 'Good to see you.' : 'Create your account.'}</h2>
            <p className="modal-intro">{modal === 'login' ? 'Sign in to keep your search moving.' : 'Join a community finding a better way to work.'}</p>
            <form className="modal-form" onSubmit={submitAuth}>
              {modal === 'register' && <><label>Full name<input name="fullName" placeholder="Aarav Sharma" required maxLength="120" /></label><label>I am a<select name="role" defaultValue="JOB_SEEKER"><option value="JOB_SEEKER">Job seeker</option><option value="EMPLOYER">Employer</option></select></label><label>Company name <span className="optional-label">(employers)</span><input name="companyName" placeholder="Your company in India" maxLength="160" /></label></>}
              <label>Email address<input type="email" name="email" placeholder="you@example.in" required /></label>
              <label>Password<input type="password" name="password" placeholder="At least 8 characters" required minLength="8" /></label>
              {error && <p className="form-error">{error}</p>}
              <button className="button button-dark modal-submit" disabled={busy}>{busy ? 'Please wait…' : modal === 'login' ? 'Sign in' : 'Create account'} <ArrowRight size={16} /></button>
            </form>
            <p className="modal-switch">{modal === 'login' ? 'New to JobHire?' : 'Already have an account?'} <button onClick={() => { setError(''); setModal(modal === 'login' ? 'register' : 'login') }}>{modal === 'login' ? 'Create an account' : 'Sign in'}</button></p>
          </> : modal === 'profile' ? <>
            <div className="section-kicker">YOUR ACCOUNT</div><h2 id="modal-title">Your profile.</h2>
            <p className="modal-intro">{profile?.email} · {profile?.role?.replace('_', ' ')}</p>
            {profile?.role === 'JOB_SEEKER' && <div className="profile-completion" aria-label={`Profile ${profileCompletion(profile)} percent complete`}>
              <div className="profile-completion-heading"><span>Profile strength</span><strong>{profileCompletion(profile)}%</strong></div>
              <div className="profile-completion-track"><span style={{ width: `${profileCompletion(profile)}%` }} /></div>
              <p>Complete your resume, skills, and education to help employers find you.</p>
            </div>}
            <form className="modal-form" onSubmit={submitProfile}>
              <label>Full name<input name="fullName" defaultValue={profile?.fullName || ''} required maxLength="120" /></label>
              {profile?.role === 'EMPLOYER' && <label>Company name<input name="companyName" defaultValue={profile?.companyName || ''} maxLength="160" /></label>}
              <label>About you<textarea name="bio" defaultValue={profile?.bio || ''} rows="4" maxLength="2000" placeholder="A little about your experience…" /></label>
              {profile?.role === 'JOB_SEEKER' && <>
                <label>Resume or portfolio link<input name="resumeUrl" type="url" defaultValue={profile?.resumeUrl || ''} maxLength="500" placeholder="https://drive.google.com/…" /></label>
                {profile.resumeUrl?.match(/^https?:\/\//i) && <a className="resume-preview" href={profile.resumeUrl} target="_blank" rel="noopener noreferrer"><BriefcaseBusiness size={15} /> Preview current resume <ArrowRight size={14} /></a>}
                <label>Skills <span className="optional-label">(separate with commas or new lines)</span><textarea name="skills" defaultValue={profile?.skills?.join(', ') || ''} rows="3" maxLength="1800" placeholder="Java, Spring Boot, PostgreSQL, React" /></label>
                <label>Education <span className="optional-label">(one qualification per line)</span><textarea name="education" defaultValue={profile?.education?.join('\n') || ''} rows="3" maxLength="1800" placeholder="B.Tech Computer Science — Anna University, 2024" /></label>
                {profile.skills?.length > 0 && <div className="profile-skills" aria-label="Saved skills">{profile.skills.map((skill) => <span key={skill}>{skill}</span>)}</div>}
              </>}
              {error && <p className="form-error">{error}</p>}<button className="button button-dark modal-submit" disabled={busy}>{busy ? 'Saving…' : 'Save profile'} <Check size={15} /></button>
            </form>
          </> : modal === 'interview' ? <>
            <div className="section-kicker">MEET YOUR NEXT TEAMMATE</div><h2 id="modal-title">Propose an interview.</h2>
            <p className="modal-intro">Choose a time for {selectedApplication?.applicantName || 'the candidate'} to review. They can accept or decline the invitation.</p>
            <form className="modal-form" onSubmit={submitInterview}>
              <label>Proposed date and time<input name="proposedAt" type="datetime-local" min={new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16)} required /></label>
              <label>Duration<select name="durationMinutes" defaultValue="30"><option value="15">15 minutes</option><option value="30">30 minutes</option><option value="45">45 minutes</option><option value="60">60 minutes</option><option value="90">90 minutes</option><option value="120">120 minutes</option><option value="240">240 minutes</option></select></label>
              <label>Meeting link <span className="optional-label">(optional, HTTP or HTTPS)</span><input name="meetingUrl" type="url" maxLength="500" placeholder="https://meet.example.com/..." /></label>
              <label>Notes <span className="optional-label">(optional)</span><textarea name="notes" rows="3" maxLength="2000" placeholder="Anything the candidate should prepare…" /></label>
              {error && <p className="form-error">{error}</p>}
              <button className="button button-dark modal-submit" disabled={busy}>{busy ? 'Sending…' : 'Send interview proposal'} <ArrowRight size={16} /></button>
            </form>
          </> : modal === 'job' ? <>
            <div className="section-kicker">FIND YOUR NEXT TEAMMATE</div><h2 id="modal-title">{editingJob ? 'Edit this role.' : 'Post a new role.'}</h2><p className="modal-intro">Share what makes this opportunity worth taking.</p>
            <form className="modal-form" onSubmit={submitJob}>
              <label>Job title<input name="title" placeholder="Java Developer" defaultValue={editingJob?.title || ''} required maxLength="160" /></label>
              <div className="form-row"><label>Location<input name="location" list="india-locations" placeholder="Bengaluru, Karnataka" defaultValue={editingJob?.location || ''} required maxLength="120" /><datalist id="india-locations"><option value="Bengaluru, Karnataka" /><option value="Mumbai, Maharashtra" /><option value="Pune, Maharashtra" /><option value="Hyderabad, Telangana" /><option value="Chennai, Tamil Nadu" /><option value="Delhi NCR" /><option value="Gurugram, Haryana" /><option value="Noida, Uttar Pradesh" /><option value="Kolkata, West Bengal" /><option value="Ahmedabad, Gujarat" /><option value="Kochi, Kerala" /><option value="Remote - India" /></datalist></label><label>Work mode<select name="workMode" defaultValue={editingJob?.workMode || 'ONSITE'}><option value="ONSITE">On-site</option><option value="HYBRID">Hybrid</option><option value="REMOTE">Remote</option></select></label></div>
              <div className="form-row"><label>Employment type<select name="employmentType" defaultValue={editingJob?.employmentType || 'Full-time'} onChange={(event) => setCompensationType(event.target.value.toLowerCase() === 'internship' ? 'stipend' : 'salary')}><option>Full-time</option><option>Part-time</option><option>Contract</option><option>Internship</option></select></label><label>Minimum experience (years)<input name="minExperienceYears" type="number" min="0" max="50" step="1" placeholder="e.g. 2" defaultValue={editingJob?.minExperienceYears ?? ''} /></label><label>Maximum experience (years)<input name="maxExperienceYears" type="number" min="0" max="50" step="1" placeholder="e.g. 5" defaultValue={editingJob?.maxExperienceYears ?? ''} /></label></div>
              {compensationType === 'stipend' ? <><div className="form-row"><label>Minimum monthly stipend (₹)<input name="stipendMinMonthly" type="number" min="1000" max="1000000" step="500" placeholder="e.g. 20000" defaultValue={editingJob?.stipendMinMonthly ?? ''} /></label><label>Maximum monthly stipend (₹)<input name="stipendMaxMonthly" type="number" min="1000" max="1000000" step="500" placeholder="e.g. 35000" defaultValue={editingJob?.stipendMaxMonthly ?? ''} /></label></div><p className="salary-hint">Optional monthly internship stipend in Indian rupees.</p></> : <><div className="form-row"><label>Minimum annual CTC (₹ LPA)<input name="salaryMinLpa" type="number" min="0.1" max="1000" step="0.1" placeholder="e.g. 8" defaultValue={editingJob?.salaryMinLpa ?? ''} /></label><label>Maximum annual CTC (₹ LPA)<input name="salaryMaxLpa" type="number" min="0.1" max="1000" step="0.1" placeholder="e.g. 12" defaultValue={editingJob?.salaryMaxLpa ?? ''} /></label></div><p className="salary-hint">Optional annual cost to company (CTC), stated in lakhs per annum. CTC is not the same as monthly take-home pay.</p></>}
              <label>Description<textarea name="description" placeholder="Tell candidates about the role…" defaultValue={editingJob?.description || ''} required rows="4" /></label>
              <label>Requirements<textarea name="requirements" placeholder="Experience, skills, and qualifications…" defaultValue={editingJob?.requirements || ''} rows="3" /></label>
              {error && <p className="form-error">{error}</p>}<button className="button button-dark modal-submit" disabled={busy}>{busy ? 'Saving…' : editingJob ? 'Save changes' : 'Publish role'} <ArrowRight size={16} /></button>
            </form>
          </> : <>
            <div className="section-kicker">TAKE THE NEXT STEP</div><h2 id="modal-title">{selectedJob?.title}</h2>
            <p className="modal-intro">{selectedJob?.companyName} · {selectedJob?.location} · {selectedJob?.employmentType}{formatSalary(selectedJob) && ` · ${formatSalary(selectedJob)}`}</p>
            <p className="job-detail">{selectedJob?.description}</p>
            {selectedJob?.requirements && <p className="job-detail"><strong>What we're looking for:</strong><br />{selectedJob.requirements}</p>}
            {session?.role === 'JOB_SEEKER' ? <form className="modal-form" onSubmit={submitApplication}><label>Cover letter <span className="optional-label">(optional)</span><textarea name="coverLetter" rows="4" placeholder="Tell them why this role speaks to you…" maxLength="5000" /></label>{error && <p className="form-error">{error}</p>}<button className="button button-dark modal-submit" disabled={busy}>{busy ? 'Sending…' : 'Submit application'} <ArrowRight size={16} /></button></form> : <div className="alert alert-error">Sign in as a job seeker to apply for this role.</div>}
          </>}
        </section>
      </div>}
    </div>
  )
}

export default App
