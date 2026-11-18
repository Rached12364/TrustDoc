"""Génère les PDF de démonstration de TrustDoc (entreprise fictive « Orbis Services »)."""
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.colors import HexColor
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak

ss = getSampleStyleSheet()
H1 = ParagraphStyle("h1", parent=ss["Title"], textColor=HexColor("#121C4E"), fontSize=20)
H2 = ParagraphStyle("h2", parent=ss["Heading2"], textColor=HexColor("#121C4E"))
P = ParagraphStyle("p", parent=ss["BodyText"], fontSize=10.5, leading=15)
TAG = ParagraphStyle("tag", parent=P, textColor=HexColor("#D0271D"), fontSize=9)

DOCS = {
    "guide_accueil.pdf": ("Guide d'accueil des nouveaux collaborateurs", "Diffusion : tous les collaborateurs", [
        [("Horaires et organisation",
          "Les horaires de travail sont flexibles entre 8 h et 18 h, avec une présence obligatoire de 10 h à 16 h. "
          "La durée de travail est de 40 heures par semaine. Les réunions d'équipe ont lieu chaque lundi à 9 h 30."),
         ("Télétravail",
          "Chaque collaborateur peut faire jusqu'à 2 jours de télétravail par semaine, après la fin de la période d'essai. "
          "La demande se fait dans l'outil RH et doit être validée par le manager. "
          "Le télétravail n'est pas possible le lundi, jour de la réunion d'équipe.")],
        [("Outils et support",
          "Le support informatique (helpdesk) répond du lundi au vendredi, de 8 h à 17 h, au poste 4400. "
          "Un ticket peut aussi être ouvert sur le portail interne. Le délai de réponse est de 4 heures ouvrées pour un incident bloquant."),
         ("Badge et accès",
          "Le badge d'accès est remis le premier jour par l'accueil. En cas de perte, il faut prévenir la sécurité dans les 24 heures "
          "pour que le badge soit désactivé.")],
    ]),
    "politique_securite.pdf": ("Politique de sécurité informatique", "Diffusion : tous les collaborateurs", [
        [("Mots de passe",
          "Un mot de passe doit contenir au moins 12 caractères, avec des majuscules, des minuscules, des chiffres et un caractère spécial. "
          "Il ne doit jamais être partagé, même avec le support informatique. Il est changé tous les 6 mois."),
         ("Double authentification",
          "La double authentification (MFA) est obligatoire pour la messagerie, le VPN et toutes les applications accessibles depuis internet.")],
        [("Hameçonnage (phishing)",
          "Un e-mail suspect ne doit jamais être ouvert. Il faut le signaler avec le bouton « Signaler » de la messagerie, "
          "ou l'envoyer à l'adresse securite@orbis-services.example. Ne cliquez sur aucun lien et n'ouvrez aucune pièce jointe."),
         ("Travail à distance",
          "En dehors des locaux, la connexion au réseau de l'entreprise passe obligatoirement par le VPN. "
          "Les réseaux Wi-Fi publics sont interdits sans VPN. Un ordinateur portable ne doit jamais rester sans surveillance.")],
    ]),
    "procedure_rh.pdf": ("Procédure RH : congés et notes de frais", "Diffusion : service RH et managers", [
        [("Congés payés",
          "Chaque collaborateur dispose de 22 jours ouvrables de congés payés par an. "
          "Une demande de congé doit être faite au moins 15 jours avant la date de départ dans l'outil RH. "
          "Au maximum 5 jours de congés peuvent être reportés sur l'année suivante."),
         ("Congés exceptionnels",
          "Des congés exceptionnels sont accordés pour les événements familiaux : 3 jours pour un mariage, 2 jours pour une naissance.")],
        [("Notes de frais",
          "Les notes de frais doivent être déposées dans un délai de 30 jours après la dépense, avec le justificatif. "
          "Le plafond pour un repas d'affaires est de 60 dinars par personne. Le remboursement est fait avec le salaire du mois suivant.")],
    ]),
    "plan_continuite.pdf": ("Plan de continuité d'activité", "Diffusion : CONFIDENTIEL — direction et administrateurs", [
        [("Objectifs de reprise",
          "En cas de sinistre, les services critiques doivent redémarrer en moins de 4 heures (RTO). "
          "La perte de données maximale acceptée est de 1 heure (RPO)."),
         ("Site de secours",
          "Le site de secours est situé dans un second centre de données, à plus de 50 km du site principal. "
          "Un test complet de bascule est réalisé deux fois par an, en mars et en octobre.")],
        [("Cellule de crise",
          "La cellule de crise est composée du directeur général, du responsable informatique et du responsable de la sécurité. "
          "Elle se réunit dans l'heure qui suit le déclenchement du plan.")],
    ]),
}

for fname, (title, scope, pages) in DOCS.items():
    doc = SimpleDocTemplate(f"{fname}", pagesize=A4, title=title, author="Orbis Services (fictif)",
                            leftMargin=60, rightMargin=60, topMargin=60, bottomMargin=60)
    story = [Paragraph("Orbis Services — entreprise fictive de démonstration", TAG),
             Paragraph(title, H1), Paragraph(scope, TAG), Spacer(1, 14)]
    for i, page in enumerate(pages):
        if i:
            story.append(PageBreak())
        for h, body in page:
            story += [Paragraph(h, H2), Paragraph(body, P), Spacer(1, 8)]
    doc.build(story)
    print("ok", fname)
