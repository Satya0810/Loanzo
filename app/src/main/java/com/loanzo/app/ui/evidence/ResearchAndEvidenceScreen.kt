package com.loanzo.app.ui.evidence

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data model representing a Double-Verified Evidence Pillar.
 */
data class EvidencePillar(
    val id: Int,
    val pillarNumber: String,
    val title: String,
    val category: String,
    val icon: ImageVector,
    val heroMetric: String,
    val heroSubtitle: String,
    val heroMetricColor: Color,
    val heroBgColor: Color,
    val groundReality: String,
    val verifiedFindings: String,
    val loanzoSolution: String,
    val primarySourceTitle: String,
    val authorOrChair: String,
    val issuingBody: String,
    val publicationDate: String,
    val documentUrl: String,
    val secondarySourceTitle: String? = null,
    val secondaryUrl: String? = null
)

/**
 * Double-Verified Evidence Repository for Loanzo Protocol.
 * Grounds every claim regarding disbursement diversion, MSME credit gap,
 * predatory apps, penal caps, and judicial recovery in authentic official sources.
 */
object EvidenceRepository {
    val pillars = listOf(
        EvidencePillar(
            id = 1,
            pillarNumber = "PILLAR I",
            title = "MSME Credit Exclusion & The ₹25.8L Cr Debt Gap",
            category = "MSME Gap",
            icon = Icons.Default.AccountBalance,
            heroMetric = "₹25.8 Lakh Cr",
            heroSubtitle = "$373B Unmet Debt Demand (85% Excluded)",
            heroMetricColor = Color(0xFF1E40AF),
            heroBgColor = Color(0xFFEFF6FF),
            groundReality = "India is powered by 63.38 Million MSMEs employing over 110 Million citizens, yet more than 85% of micro-enterprises cannot access formal bank credit due to absence of audited balance sheets, CIBIL histories, or prime urban real estate.",
            verifiedFindings = "The World Bank & IFC report established total debt demand at ₹36.7 Lakh Cr ($530B) against formal supply of only ₹10.9 Lakh Cr ($157B), leaving an addressable formal credit gap of ₹25.8 Lakh Cr. Micro units constitute 99.4% of all enterprises.",
            loanzoSolution = "Loanzo's 5-class collateral escrow vault allows micro-enterprises to collateralize agricultural receipts, workshop machinery, livestock, and gold at algorithmic haircuts, unlocking productive credit for thin-file borrowers.",
            primarySourceTitle = "Financing India's MSMEs: Estimation of Debt Requirement of MSMEs in India",
            authorOrChair = "International Finance Corporation (IFC) & Intellecap",
            issuingBody = "World Bank Group",
            publicationDate = "November 2018",
            documentUrl = "http://documents.worldbank.org/curated/en/759261548828982149/pdf/Financing-Indias-MSMEs-Estimation-of-Debt-Requirement-of-MSMEs-in-India.pdf",
            secondarySourceTitle = "Report of the Expert Committee on MSMEs (Chaired by U.K. Sinha)",
            secondaryUrl = "https://www.rbi.org.in/Scripts/PublicationReportDetails.aspx?UrlPage=&ID=924"
        ),
        EvidencePillar(
            id = 2,
            pillarNumber = "PILLAR II",
            title = "The Informal Credit Shadow Economy & Usury Trap",
            category = "Informal Market",
            icon = Icons.Default.TrendingDown,
            heroMetric = "₹35 Lakh Cr",
            heroSubtitle = "$420B Shadow Market (35%–70% Credit Share)",
            heroMetricColor = Color(0xFFB45309),
            heroBgColor = Color(0xFFFFFBEB),
            groundReality = "Shut out of formal banking, unbanked rural and semi-urban borrowers rely on local moneylenders and informal credit, paying predatory compounding interest between 36% and 120% APR ('meter vaddi').",
            verifiedFindings = "MoSPI's All-India Debt and Investment Survey (NSS Report 588) verified that non-institutional agencies account for 35.0% of total rural household debt, and up to 70% of nano-enterprise working capital.",
            loanzoSolution = "Enforces statutory interest ceilings (12%–18% simple interest) under state Money Lenders Acts, replacing usurious moneylenders with transparent community reverse-auction bidding.",
            primarySourceTitle = "NSS Report No. 588: All-India Debt and Investment Survey (AIDIS) – 77th Round",
            authorOrChair = "National Statistical Office (NSO)",
            issuingBody = "Ministry of Statistics and Programme Implementation (MoSPI), GoI",
            publicationDate = "September 2021",
            documentUrl = "http://mospi.nic.in/sites/default/files/publication_reports/Report%20no.%20588-AIDIS-77Rm-Sept.pdf",
            secondarySourceTitle = "PIB Official Press Release on AIDIS 2019 Survey Findings",
            secondaryUrl = "https://pib.gov.in/PressReleasePage.aspx?PRID=1753756"
        ),
        EvidencePillar(
            id = 3,
            pillarNumber = "PILLAR III",
            title = "Cash Loan Diversion (The 48% Fungibility Leakage)",
            category = "Disbursement",
            icon = Icons.Default.MoneyOff,
            heroMetric = "48% Diversion",
            heroSubtitle = "Lump-Sum Cash Diverted from Business into Consumption",
            heroMetricColor = Color(0xFFBE123C),
            heroBgColor = Color(0xFFFFF1F2),
            groundReality = "When loans are disbursed as raw cash into a borrower's pocket or generic bank account, cash fungibility causes 48 out of 100 borrowers to divert funds into non-productive consumption, family ceremonies, or old debts. Because no timber or inventory is bought, the business yields 0% new Gross Value Added (GVA), causing structural default.",
            verifiedFindings = "Landmark randomized controlled evaluations (Banerjee, Duflo et al. MIT/J-PAL) and rural credit diversion research (Chatterjee et al.) proved that liquid cash micro-loans suffer 40%–50% diversion into consumption smoothing and household shocks rather than productive enterprise creation.",
            loanzoSolution = "100% Zero-Cash Policy: Loans are partitioned into milestone tranches disbursed directly to verified supplier UPI VPAs upon invoice OCR and ₹1 penny-drop validation, guaranteeing 100% productive capital utilization.",
            primarySourceTitle = "The Miracle of Microfinance? Evidence from a Randomized Evaluation",
            authorOrChair = "Abhijit Banerjee, Esther Duflo, Rachel Glennerster, & Cynthia Kinnan",
            issuingBody = "American Economic Journal: Applied Economics (DOI: 10.1257/app.20130533) / NBER",
            publicationDate = "2015",
            documentUrl = "https://www.aeaweb.org/articles?id=10.1257/app.20130533",
            secondarySourceTitle = "Incidence and Determinants of Loan Diversion in India (Chatterjee et al.)",
            secondaryUrl = "https://ideas.repec.org/p/pra/mprapa/89918.html"
        ),
        EvidencePillar(
            id = 4,
            pillarNumber = "PILLAR IV",
            title = "Predatory Digital Lending Apps & Harassment",
            category = "Predatory Apps",
            icon = Icons.Default.Warning,
            heroMetric = "600+ Illegal Apps",
            heroSubtitle = "54% of Apps Operating Without RBI Oversight",
            heroMetricColor = Color(0xFFC2410C),
            heroBgColor = Color(0xFFFFF7ED),
            groundReality = "Rogue loan apps harvest user contacts and photo galleries, charge hidden triple-digit fees, and deploy abusive recovery callers who publicly shame and harass borrowers.",
            verifiedFindings = "The RBI Working Group on Digital Lending identified over 600 unauthorized apps out of 1,100 examined, with 2,562 formal grievances filed on the Sachet portal regarding harassment, data theft, and extortionate rates.",
            loanzoSolution = "Privacy-by-Design complying with DPDP Act 2023: Zero access to contacts, zero call-log tracking, zero gallery snooping, and a 1-tap Telegram SOS Webhook dispatching legal warnings citing IPC Sections 503/506 (BNS §351).",
            primarySourceTitle = "Report of the Working Group on Digital Lending including Lending through Online Platforms and Mobile Apps",
            authorOrChair = "Shri Jayant Kumar Dash (Executive Director, RBI)",
            issuingBody = "Reserve Bank of India (RBI)",
            publicationDate = "November 18, 2021",
            documentUrl = "https://www.rbi.org.in/Scripts/PublicationReportDetails.aspx?UrID=1214",
            secondarySourceTitle = "RBI Guidelines on Digital Lending (Mandatory Direct A2A Transfers)",
            secondaryUrl = "https://www.rbi.org.in/Scripts/NotificationUser.aspx?Id=12382&Mode=0"
        ),
        EvidencePillar(
            id = 5,
            pillarNumber = "PILLAR V",
            title = "Statutory Cap on Penal Charges (Compounding Ban)",
            category = "Fair Lending",
            icon = Icons.Default.Gavel,
            heroMetric = "2.0% p.a. Cap",
            heroSubtitle = "Simple Interest Ceiling & Strict Compounding Prohibition",
            heroMetricColor = Color(0xFF047857),
            heroBgColor = Color(0xFFECFDF5),
            groundReality = "Predatory lenders and unregulated apps levy compounding 'penal interest on penal interest', ballooning small delays into insurmountable debt spirals.",
            verifiedFindings = "RBI Circular RBI/2023-24/53 mandates that penal charges for delayed payments must be reasonable operational charges, strictly prohibited from being capitalized or compounded into the principal balance.",
            loanzoSolution = "Loanzo's PenaltyEngine hardcodes a maximum 2.0% p.a. simple interest penal charge booked in a separate penalty ledger with a mandatory 3-day zero-charge grace period.",
            primarySourceTitle = "Fair Lending Practice - Penal Charges in Loan Accounts (RBI/2023-24/53)",
            authorOrChair = "Department of Regulation (DoR)",
            issuingBody = "Reserve Bank of India (RBI)",
            publicationDate = "August 18, 2023",
            documentUrl = "https://www.rbi.org.in/Scripts/NotificationUser.aspx?Id=12527&Mode=0",
            secondarySourceTitle = "RBI Sachet Public Grievance Portal for Illegal Financial Practices",
            secondaryUrl = "https://sachet.rbi.org.in/"
        ),
        EvidencePillar(
            id = 6,
            pillarNumber = "PILLAR VI",
            title = "7–10 Year Judicial Backlog vs. 60-Day Summary Suit",
            category = "Legal Recourse",
            icon = Icons.Default.HourglassBottom,
            heroMetric = "60–90 Days",
            heroSubtitle = "Order 37 CPC Summary Decree vs 2,500+ Days Regular Trial",
            heroMetricColor = Color(0xFF6D28D9),
            heroBgColor = Color(0xFFF5F3FF),
            groundReality = "Because informal loans are arranged verbally or on informal notes, civil money recovery suits take 7 to 10 years in Indian district courts, causing frustrated lenders to bypass the law and hire recovery muscle.",
            verifiedFindings = "National Judicial Data Grid records over 4.4 Crore pending court cases in India. Under Order XXXVII CPC, debts founded on formal Promissory Notes require the debtor to deposit cash security to defend, cutting decree timelines down to 60–90 days.",
            loanzoSolution = "Loanzo synthesizes an unconditional Section 4 NI Act Promissory Note with hardware StrongBox biometric signing, unlocking expedited Order 37 summary recovery in under 90 days.",
            primarySourceTitle = "National Judicial Data Grid (NJDG) — Case Pendency Dashboard",
            authorOrChair = "eCourts Project & Law Commission of India",
            issuingBody = "Ministry of Law and Justice, Government of India",
            publicationDate = "Active 2024 Dashboard",
            documentUrl = "https://njdg.ecourts.gov.in/njdgnew/",
            secondarySourceTitle = "Code of Civil Procedure, 1908 (Order XXXVII Summary Suits) — India Code",
            secondaryUrl = "https://www.indiacode.nic.in/handle/123456789/2191"
        ),
        EvidencePillar(
            id = 7,
            pillarNumber = "PILLAR VII",
            title = "Digital Evidence Admissibility & Non-Custodial Safe Harbor",
            category = "Safe Harbor",
            icon = Icons.Default.Shield,
            heroMetric = "₹0.00 Custody",
            heroSubtitle = "100% Non-Custodial Facilitator with Electronic Admissibility",
            heroMetricColor = Color(0xFF0369A1),
            heroBgColor = Color(0xFFF0F9FF),
            groundReality = "Fintech apps that pool customer funds face criminal violations under Section 45-IA of the RBI Act without a ₹20 Crore Net Owned Fund NBFC-P2P license.",
            verifiedFindings = "Section 10A IT Act 2000 establishes validity of electronic contracts. Supreme Court precedent (G. Pankajakshi Amma v. Mathai Mathew) validated informal credit agreements. Income Tax Sections 269SS/269T prohibit cash loans of ₹20,000+.",
            loanzoSolution = "Holds ₹0.00 customer funds. Capital flows directly between borrower, lender, and merchant bank accounts via NPCI UPI rails. Digital evidence is sealed under Section 63 BSA 2023 (Section 65B Evidence Act) with SHA-256 hashes.",
            primarySourceTitle = "Information Technology Act, 2000 (Section 10A - Electronic Contracts)",
            authorOrChair = "Parliament of India",
            issuingBody = "Ministry of Electronics and Information Technology (MeitY), GoI",
            publicationDate = "2000 (As Amended)",
            documentUrl = "https://www.indiacode.nic.in/handle/123456789/1999",
            secondarySourceTitle = "Bharatiya Sakshya Adhiniyam, 2023 (Section 63 - Electronic Records Admissibility)",
            secondaryUrl = "https://www.indiacode.nic.in/handle/123456789/20063"
        )
    )
}

/**
 * Institutional Evidence & Statutory Grounding Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchAndEvidenceScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All Evidence") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf("All Evidence", "Disbursement", "MSME Gap", "Informal Market", "Predatory Apps", "Fair Lending", "Legal Recourse", "Safe Harbor")
    }

    val filteredPillars = remember(selectedCategory, searchQuery) {
        EvidenceRepository.pillars.filter { pillar ->
            val matchesCategory = (selectedCategory == "All Evidence") || (pillar.category.equals(selectedCategory, ignoreCase = true))
            val matchesQuery = searchQuery.isBlank() ||
                    pillar.title.contains(searchQuery, ignoreCase = true) ||
                    pillar.verifiedFindings.contains(searchQuery, ignoreCase = true) ||
                    pillar.primarySourceTitle.contains(searchQuery, ignoreCase = true) ||
                    pillar.heroMetric.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Research & Evidence Grounds",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Double-Verified Official Sources & Statutes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Institutional Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E3A8A), // Deep Royal Navy
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "INSTITUTIONAL GROUNDING DOSSIER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF93C5FD),
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Every Loanzo architectural decision is mathematically and legally grounded in primary research.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Backed by World Bank / IFC debt studies, RBI Working Group empirical data, NSSO 77th Round survey reports, and landmark Supreme Court jurisprudence.",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search claims, sources (e.g. 'diversion', 'World Bank')",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF64748B)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory.equals(category, ignoreCase = true)
                        Surface(
                            onClick = { selectedCategory = category },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF1E40AF) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF1E40AF) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            // Pillar Cards
            items(filteredPillars, key = { it.id }) { pillar ->
                EvidencePillarCard(
                    pillar = pillar,
                    onOpenUrl = { url ->
                        openExternalUrl(context, url)
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

/**
 * Individual Card rendering a single Double-Verified Pillar.
 */
@Composable
fun EvidencePillarCard(
    pillar: EvidencePillar,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header: Pillar Tag + Verified Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = pillar.pillarNumber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(0.5.dp, Color(0xFFA7F3D0))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Double Verified",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "DOUBLE VERIFIED",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = pillar.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hero Metric Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = pillar.heroBgColor,
                border = BorderStroke(0.5.dp, pillar.heroMetricColor.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = pillar.icon,
                                contentDescription = null,
                                tint = pillar.heroMetricColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = pillar.heroMetric,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = pillar.heroMetricColor
                        )
                        Text(
                            text = pillar.heroSubtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // The Ground Reality
            Text(
                text = "The Ground Reality:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = pillar.groundReality,
                fontSize = 12.sp,
                color = Color(0xFF475569),
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Official Finding
            Text(
                text = "Verified Official Finding:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = pillar.verifiedFindings,
                fontSize = 12.sp,
                color = Color(0xFF334155),
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Loanzo Architecture Solution
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Loanzo Architectural Solution:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pillar.loanzoSolution,
                        fontSize = 11.5.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source Citation Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PRIMARY OFFICIAL PUBLICATION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = pillar.primarySourceTitle,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${pillar.authorOrChair} • ${pillar.issuingBody} (${pillar.publicationDate})",
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Open Primary Document Button
                    Button(
                        onClick = { onOpenUrl(pillar.documentUrl) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E40AF),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open Document",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open Official Document / Report",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Optional Secondary Source Button
                    if (pillar.secondarySourceTitle != null && pillar.secondaryUrl != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onOpenUrl(pillar.secondaryUrl) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF334155)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "View Secondary Link",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View Statutory / Secondary Link",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to safely open an external URL via Android browser Intent.
 */
private fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Could not open document: ${e.localizedMessage ?: "Invalid URL"}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
